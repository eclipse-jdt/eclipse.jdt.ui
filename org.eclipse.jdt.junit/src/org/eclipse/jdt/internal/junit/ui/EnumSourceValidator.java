/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeStyleConfiguration;

/**
 * Utility class for validating and modifying {@code @EnumSource} annotations.
 *
 * <p>Provides methods to:
 * <ul>
 * <li>Check if a method uses {@code @EnumSource} with EXCLUDE mode</li>
 * <li>Get currently excluded names from an {@code @EnumSource} annotation</li>
 * <li>Remove exclude mode (re-include all)</li>
 * <li>Remove a single value from the exclusion list</li>
 * </ul>
 *
 * @since 3.15
 */
public class EnumSourceValidator {

	private static final String ENUM_SOURCE_ANNOTATION= "org.junit.jupiter.params.provider.EnumSource"; //$NON-NLS-1$
	private static final String ENUM_SOURCE_MODE_EXCLUDE= "EXCLUDE"; //$NON-NLS-1$
	private static final String MEMBER_VALUE= "value"; //$NON-NLS-1$
	private static final String MEMBER_MODE= "mode"; //$NON-NLS-1$
	private static final String MEMBER_NAMES= "names"; //$NON-NLS-1$

	/** Regex matching the JUnit 5 default parameterized test display-name prefix {@code "[N] "}. */
	private static final String DISPLAY_NAME_INDEX_PREFIX_REGEX= "^\\[\\d+\\]\\s*"; //$NON-NLS-1$

	/**
	 * Returns whether the given method has an {@code @EnumSource} annotation with
	 * {@code mode = Mode.EXCLUDE} and a non-empty names array.
	 *
	 * @param method the method to check
	 * @return <code>true</code> if EXCLUDE mode is active
	 * @throws JavaModelException if there is an error accessing the Java model
	 */
	public static boolean isExcludeMode(IMethod method) throws JavaModelException {
		IAnnotationBinding binding= findEnumSourceBinding(method);
		if (binding == null) {
			return false;
		}
		return ENUM_SOURCE_MODE_EXCLUDE.equals(getMode(binding));
	}

	/**
	 * Returns the list of currently excluded enum constant names from {@code @EnumSource}.
	 * Returns an empty list if there are no exclusions or the method does not have
	 * an {@code @EnumSource} annotation in EXCLUDE mode.
	 *
	 * @param method the method to inspect
	 * @return list of excluded names (never <code>null</code>)
	 * @throws JavaModelException if there is an error accessing the Java model
	 */
	public static List<String> getExcludedNames(IMethod method) throws JavaModelException {
		IAnnotationBinding binding= findEnumSourceBinding(method);
		if (binding == null || !ENUM_SOURCE_MODE_EXCLUDE.equals(getMode(binding))) {
			return new ArrayList<>();
		}
		return getNames(binding);
	}

	/**
	 * Returns the fully qualified name of the enum type declared in {@code @EnumSource},
	 * or <code>null</code> if none is declared explicitly.
	 *
	 * @param method the method to inspect
	 * @return the enum type FQN, or <code>null</code>
	 * @throws JavaModelException if there is an error accessing the Java model
	 */
	public static String getEnumTypeName(IMethod method) throws JavaModelException {
		IAnnotationBinding binding= findEnumSourceBinding(method);
		if (binding == null) {
			return null;
		}
		for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
			if (MEMBER_VALUE.equals(pair.getName())) {
				Object val= pair.getValue();
				if (val instanceof ITypeBinding) {
					return ((ITypeBinding) val).getQualifiedName();
				}
			}
		}
		// try to infer from method parameter
		try {
			ICompilationUnit cu= method.getCompilationUnit();
			if (cu != null) {
				ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
				parser.setSource(cu);
				parser.setResolveBindings(true);
				CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
				final String[] result= { null };
				astRoot.accept(new ASTVisitor() {
					@Override
					public boolean visit(MethodDeclaration node) {
						if (node.getName().getIdentifier().equals(method.getElementName())) {
							IMethodBinding mb= node.resolveBinding();
							if (mb != null && mb.getParameterTypes().length > 0) {
								ITypeBinding paramType= mb.getParameterTypes()[0];
								if (paramType != null && paramType.isEnum()) {
									result[0]= paramType.getQualifiedName();
								}
							}
						}
						return false;
					}
				});
				return result[0];
			}
		} catch (Exception e) {
			// fall through
		}
		return null;
	}

	/**
	 * Removes all exclusions from {@code @EnumSource} by removing the {@code mode} and
	 * {@code names} attributes. Also removes the {@code Mode} import if it is no longer
	 * needed.
	 *
	 * @param method the method to modify
	 * @throws JavaModelException if there is an error accessing the Java model
	 */
	public static void removeExcludeMode(IMethod method) throws JavaModelException {
		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		final boolean[] modified= { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (!node.getName().getIdentifier().equals(method.getElementName())) {
					return false;
				}
				for (Object modifier : node.modifiers()) {
					if (modifier instanceof NormalAnnotation) {
						NormalAnnotation annotation= (NormalAnnotation) modifier;
						IAnnotationBinding annBinding= annotation.resolveAnnotationBinding();
						if (annBinding == null) continue;
						ITypeBinding annType= annBinding.getAnnotationType();
						if (annType == null || !ENUM_SOURCE_ANNOTATION.equals(annType.getQualifiedName())) continue;

						// Collect the value pair (if any), remove mode and names
						MemberValuePair valuePair= null;
						List<?> values= annotation.values();
						for (Object v : values) {
							MemberValuePair mvp= (MemberValuePair) v;
							if (MEMBER_VALUE.equals(mvp.getName().getIdentifier())) {
								valuePair= mvp;
							}
						}

						// Build replacement: NormalAnnotation with only value (if present)
						NormalAnnotation newAnnotation= ast.newNormalAnnotation();
						newAnnotation.setTypeName(ast.newName("EnumSource")); //$NON-NLS-1$
						if (valuePair != null) {
							MemberValuePair newValuePair= ast.newMemberValuePair();
							newValuePair.setName(ast.newSimpleName(MEMBER_VALUE));
							newValuePair.setValue((org.eclipse.jdt.core.dom.Expression) rewrite.createCopyTarget(valuePair.getValue()));
							newAnnotation.values().add(newValuePair);
						}

						rewrite.replace(annotation, newAnnotation, null);
						modified[0]= true;
						break;
					}
				}
				return false;
			}
		});

		if (modified[0]) {
			applyChangesWithModeImportCleanup(cu, astRoot, rewrite);
		}
	}

	/**
	 * Removes a single enum constant name from the {@code @EnumSource} exclusion list.
	 * If the exclusion list becomes empty after the removal, removes both {@code mode} and
	 * {@code names} attributes entirely.
	 *
	 * @param method the method to modify
	 * @param enumValueName the enum constant name to re-include
	 * @throws JavaModelException if there is an error accessing the Java model
	 */
	public static void removeValueFromExclusion(IMethod method, String enumValueName) throws JavaModelException {
		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		List<String> currentExcluded= getExcludedNames(method);
		List<String> newNames= new ArrayList<>(currentExcluded);
		newNames.remove(enumValueName);

		if (newNames.isEmpty()) {
			removeExcludeMode(method);
			return;
		}

		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		final boolean[] modified= { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (!node.getName().getIdentifier().equals(method.getElementName())) {
					return false;
				}
				for (Object modifier : node.modifiers()) {
					if (modifier instanceof NormalAnnotation) {
						NormalAnnotation annotation= (NormalAnnotation) modifier;
						IAnnotationBinding annBinding= annotation.resolveAnnotationBinding();
						if (annBinding == null) continue;
						ITypeBinding annType= annBinding.getAnnotationType();
						if (annType == null || !ENUM_SOURCE_ANNOTATION.equals(annType.getQualifiedName())) continue;

						// Find the names member value pair and remove the value
						List<?> values= annotation.values();
						for (Object v : values) {
							MemberValuePair mvp= (MemberValuePair) v;
							if (MEMBER_NAMES.equals(mvp.getName().getIdentifier())) {
								if (mvp.getValue() instanceof ArrayInitializer) {
									ArrayInitializer arr= (ArrayInitializer) mvp.getValue();
									ListRewrite listRewrite= rewrite.getListRewrite(arr, ArrayInitializer.EXPRESSIONS_PROPERTY);
									List<?> expressions= arr.expressions();
									for (Object expr : expressions) {
										if (expr instanceof StringLiteral) {
											StringLiteral sl= (StringLiteral) expr;
											if (enumValueName.equals(sl.getLiteralValue())) {
												listRewrite.remove(sl, null);
												modified[0]= true;
												break;
											}
										}
									}
								}
								break;
							}
						}
						break;
					}
				}
				return false;
			}
		});

		if (modified[0]) {
			applySimpleChanges(cu, astRoot, rewrite);
		}
	}

	// --- helpers ---

	private static IAnnotationBinding findEnumSourceBinding(IMethod method) throws JavaModelException {
		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return null;
		}
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		final IAnnotationBinding[] result= { null };
		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (!node.getName().getIdentifier().equals(method.getElementName())) {
					return false;
				}
				IMethodBinding mb= node.resolveBinding();
				if (mb == null) return false;
				for (IAnnotationBinding ann : mb.getAnnotations()) {
					ITypeBinding annType= ann.getAnnotationType();
					if (annType != null && ENUM_SOURCE_ANNOTATION.equals(annType.getQualifiedName())) {
						result[0]= ann;
						break;
					}
				}
				return false;
			}
		});
		return result[0];
	}

	private static String getMode(IAnnotationBinding binding) {
		for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
			if (MEMBER_MODE.equals(pair.getName())) {
				Object val= pair.getValue();
				if (val instanceof IVariableBinding) {
					return ((IVariableBinding) val).getName();
				}
			}
		}
		return null; // default INCLUDE
	}

	private static List<String> getNames(IAnnotationBinding binding) {
		List<String> result= new ArrayList<>();
		for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
			if (MEMBER_NAMES.equals(pair.getName())) {
				Object val= pair.getValue();
				if (val instanceof Object[]) {
					for (Object item : (Object[]) val) {
						if (item instanceof String) {
							result.add((String) item);
						}
					}
				} else if (val instanceof String) {
					result.add((String) val);
				}
			}
		}
		return result;
	}

	private static void applySimpleChanges(ICompilationUnit cu, CompilationUnit astRoot, ASTRewrite rewrite) {
		try {
			TextEdit rewriteEdit= rewrite.rewriteAST();
			cu.applyTextEdit(rewriteEdit, null);
			cu.save(null, true);
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	private static void applyChangesWithModeImportCleanup(ICompilationUnit cu, CompilationUnit astRoot, ASTRewrite rewrite) {
		try {
			MultiTextEdit multiEdit= new MultiTextEdit();

			// Remove "EnumSource.Mode" or "Mode" import if no longer referenced
			ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(astRoot, true);
			importRewrite.removeImport("org.junit.jupiter.params.provider.EnumSource.Mode"); //$NON-NLS-1$

			TextEdit importEdit= importRewrite.rewriteImports(null);
			if (importEdit.hasChildren()) {
				multiEdit.addChild(importEdit);
			}
			TextEdit rewriteEdit= rewrite.rewriteAST();
			if (rewriteEdit.hasChildren()) {
				multiEdit.addChild(rewriteEdit);
			}
			if (multiEdit.hasChildren()) {
				cu.applyTextEdit(multiEdit, null);
				cu.save(null, true);
			}
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	/**
	 * Extracts the enum constant name from a parameterized test display name.
	 * <p>
	 * JUnit 5 default format is {@code "[N] CONSTANT_NAME"}. This method strips
	 * the leading {@code "[N] "} prefix. If the display name already looks like a plain
	 * constant name (no prefix), it is returned as-is.
	 *
	 * @param displayName the display name of the test case element
	 * @return the extracted enum constant name
	 */
	public static String extractEnumConstantFromDisplayName(String displayName) {
		if (displayName == null) {
			return null;
		}
		// Strip "[N] " prefix (JUnit default parameterized name format)
		String stripped= displayName.replaceFirst(DISPLAY_NAME_INDEX_PREFIX_REGEX, "").trim(); //$NON-NLS-1$
		return stripped.isEmpty() ? displayName : stripped;
	}

	/**
	 * Returns whether the given method has an {@code @EnumSource} annotation.
	 *
	 * @param method the method to check
	 * @return <code>true</code> if the method declares {@code @EnumSource}
	 * @throws JavaModelException if there is an error accessing the Java model
	 */
	public static boolean hasEnumSource(IMethod method) throws JavaModelException {
		return findEnumSourceBinding(method) != null;
	}

	/**
	 * Returns a copy of the excluded names after removing the given value.
	 * Useful for validation before committing the change.
	 *
	 * @param excludedNames current list of excluded names
	 * @param valueToRemove the value that would be re-included
	 * @return new list without the removed value
	 */
	public static List<String> computeNamesAfterReinclude(List<String> excludedNames, String valueToRemove) {
		List<String> result= new ArrayList<>(excludedNames);
		result.remove(valueToRemove);
		return result;
	}

	private EnumSourceValidator() {
		// Utility class - no instances
	}
}
