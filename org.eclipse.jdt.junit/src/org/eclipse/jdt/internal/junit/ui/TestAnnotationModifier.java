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

import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
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
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

import org.eclipse.jdt.ui.CodeStyleConfiguration;

/**
 * Utility class for modifying JUnit test annotations.
 * Provides common functionality for adding, removing, and modifying test annotations
 * to avoid code duplication across Quick Assist and context menu actions.
 *
 * <p><b>Note on Method Matching:</b> This utility matches methods by name only when
 * traversing the AST. This is acceptable for JUnit test methods because:
 * <ul>
 * <li>Test methods are typically not overloaded (JUnit best practices)</li>
 * <li>The IMethod parameter comes from the test runner which already identifies the exact method</li>
 * <li>The AST traversal is just to find the AST node for that specific IMethod</li>
 * </ul>
 *
 * @since 3.15
 */
public class TestAnnotationModifier {

	private static final String JUNIT4_IGNORE_ANNOTATION= "org.junit.Ignore"; //$NON-NLS-1$
	private static final String JUNIT5_DISABLED_ANNOTATION= "org.junit.jupiter.api.Disabled"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_ANNOTATION= "org.junit.jupiter.api.Test"; //$NON-NLS-1$
	private static final String JUNIT5_PARAMETERIZED_TEST_ANNOTATION= "org.junit.jupiter.params.ParameterizedTest"; //$NON-NLS-1$
	private static final String JUNIT5_REPEATED_TEST_ANNOTATION= "org.junit.jupiter.api.RepeatedTest"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_FACTORY_ANNOTATION= "org.junit.jupiter.api.TestFactory"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_TEMPLATE_ANNOTATION= "org.junit.jupiter.api.TestTemplate"; //$NON-NLS-1$

	private static final String JUNIT5_ENUM_SOURCE_ANNOTATION= "org.junit.jupiter.params.provider.EnumSource"; //$NON-NLS-1$
	private static final String JUNIT5_ENUM_SOURCE_MODE_FQN= "org.junit.jupiter.params.provider.EnumSource.Mode"; //$NON-NLS-1$

	/**
	 * Add @Disabled (JUnit 5) or @Ignore (JUnit 4) annotation to a method.
	 *
	 * @param method the method to add the annotation to
	 * @param isJUnit5 whether to use JUnit 5 (@Disabled) or JUnit 4 (@Ignore)
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void addDisabledAnnotation(IMethod method, boolean isJUnit5) throws JavaModelException {
		String annotationQualifiedName= isJUnit5 ? JUNIT5_DISABLED_ANNOTATION : JUNIT4_IGNORE_ANNOTATION;
		String annotationSimpleName= isJUnit5 ? "Disabled" : "Ignore"; //$NON-NLS-1$ //$NON-NLS-2$

		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		// Parse the compilation unit
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		final boolean[] modified= new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					// Add the annotation
					org.eclipse.jdt.core.dom.MarkerAnnotation annotation= ast.newMarkerAnnotation();
					annotation.setTypeName(ast.newName(annotationSimpleName));

					ListRewrite listRewrite= rewrite.getListRewrite(node, MethodDeclaration.MODIFIERS2_PROPERTY);
					listRewrite.insertFirst(annotation, null);
					modified[0]= true;
				}
				return false;
			}
		});

		if (modified[0]) {
			applyChanges(cu, astRoot, rewrite, annotationQualifiedName);
		}
	}

	/**
	 * Remove @Disabled or @Ignore annotation from a method.
	 *
	 * @param method the method to remove the annotation from
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void removeDisabledAnnotation(IMethod method) throws JavaModelException {
		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		// Parse the compilation unit
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		final boolean[] modified= new boolean[] { false };
		final Annotation[] removedAnnotationNode= new Annotation[1];

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					IMethodBinding methodBinding= node.resolveBinding();
					if (methodBinding != null) {
						IAnnotationBinding[] annotations= methodBinding.getAnnotations();
						for (IAnnotationBinding annotationBinding : annotations) {
							ITypeBinding annotationType= annotationBinding.getAnnotationType();
							if (annotationType != null) {
								String qualifiedName= annotationType.getQualifiedName();
								if (JUNIT5_DISABLED_ANNOTATION.equals(qualifiedName) ||
									JUNIT4_IGNORE_ANNOTATION.equals(qualifiedName)) {
									// Find and remove the annotation
									List<?> modifiers= node.modifiers();
									for (Object modifier : modifiers) {
										if (modifier instanceof Annotation) {
											Annotation annotation= (Annotation) modifier;
											if (annotation.resolveAnnotationBinding() == annotationBinding) {
												ListRewrite listRewrite= rewrite.getListRewrite(node, MethodDeclaration.MODIFIERS2_PROPERTY);
												listRewrite.remove(annotation, null);
												removedAnnotationNode[0]= annotation;
												modified[0]= true;
												break;
											}
										}
									}
									break;
								}
							}
						}
					}
				}
				return false;
			}
		});

		if (modified[0]) {
			applyChangesWithImportRemoval(cu, astRoot, rewrite, removedAnnotationNode[0]);
		}
	}

	/**
	 * Check if a method has @Disabled or @Ignore annotation.
	 *
	 * @param method the method to check
	 * @return true if the method has @Disabled or @Ignore annotation
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static boolean isDisabled(IMethod method) throws JavaModelException {
		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return false;
		}

		// Parse the compilation unit
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		final boolean[] result= new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					IMethodBinding methodBinding= node.resolveBinding();
					if (methodBinding != null) {
						IAnnotationBinding[] annotations= methodBinding.getAnnotations();
						for (IAnnotationBinding annotationBinding : annotations) {
							ITypeBinding annotationType= annotationBinding.getAnnotationType();
							if (annotationType != null) {
								String qualifiedName= annotationType.getQualifiedName();
								if (JUNIT5_DISABLED_ANNOTATION.equals(qualifiedName) ||
									JUNIT4_IGNORE_ANNOTATION.equals(qualifiedName)) {
									result[0]= true;
									break;
								}
							}
						}
					}
				}
				return false;
			}
		});

		return result[0];
	}

	/**
	 * Check if method has JUnit 5 test annotations.
	 */
	public static boolean isJUnit5TestMethod(MethodDeclaration methodDecl) {
		IMethodBinding binding= methodDecl.resolveBinding();
		if (binding == null) {
			return false;
		}

		IAnnotationBinding[] annotations= binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			ITypeBinding annotationType= annotation.getAnnotationType();
			if (annotationType != null) {
				String qualifiedName= annotationType.getQualifiedName();
				if (isJUnit5TestAnnotation(qualifiedName)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Check if method has specific annotation.
	 */
	public static boolean hasAnnotation(MethodDeclaration methodDecl, String annotationQualifiedName) {
		IMethodBinding binding= methodDecl.resolveBinding();
		if (binding == null) {
			return false;
		}

		IAnnotationBinding[] annotations= binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			ITypeBinding annotationType= annotation.getAnnotationType();
			if (annotationType != null && annotationQualifiedName.equals(annotationType.getQualifiedName())) {
				return true;
			}
		}

		return false;
	}

	private static boolean isJUnit5TestAnnotation(String qualifiedName) {
		return JUNIT5_TEST_ANNOTATION.equals(qualifiedName) ||
			   JUNIT5_PARAMETERIZED_TEST_ANNOTATION.equals(qualifiedName) ||
			   JUNIT5_REPEATED_TEST_ANNOTATION.equals(qualifiedName) ||
			   JUNIT5_TEST_FACTORY_ANNOTATION.equals(qualifiedName) ||
			   JUNIT5_TEST_TEMPLATE_ANNOTATION.equals(qualifiedName);
	}

	private static void applyChanges(ICompilationUnit cu, CompilationUnit astRoot, ASTRewrite rewrite, String annotationToImport) {
		try {
			MultiTextEdit multiEdit= new MultiTextEdit();

			// Add import if needed
			if (annotationToImport != null) {
				ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(astRoot, true);
				importRewrite.addImport(annotationToImport);

				TextEdit importEdit= importRewrite.rewriteImports(null);
				if (importEdit.hasChildren() || importEdit.getLength() != 0) {
					multiEdit.addChild(importEdit);
				}
			}

			TextEdit rewriteEdit= rewrite.rewriteAST();
			multiEdit.addChild(rewriteEdit);

			// Apply the combined edit
			cu.applyTextEdit(multiEdit, null);
			cu.save(null, true);
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	/**
	 * Adds an enum constant name to the {@code @EnumSource} exclusion list on the given method.
	 *
	 * <p>If the annotation currently has no mode (defaults to {@code INCLUDE}), the mode is
	 * changed to {@code EXCLUDE} and the value is added to the names list. If the annotation
	 * already uses {@code EXCLUDE} mode, the value is appended to the existing names.
	 *
	 * @param method the test method that carries the {@code @EnumSource} annotation
	 * @param enumValueName the enum constant name to exclude
	 * @throws JavaModelException if there is an error accessing the Java model
	 * @since 3.15
	 */
	public static void excludeEnumValue(IMethod method, String enumValueName) throws JavaModelException {
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
					if (modifier instanceof Annotation) {
						Annotation annotation= (Annotation) modifier;
						IAnnotationBinding annBinding= annotation.resolveAnnotationBinding();
						if (annBinding == null) continue;
						ITypeBinding annType= annBinding.getAnnotationType();
						if (annType == null || !JUNIT5_ENUM_SOURCE_ANNOTATION.equals(annType.getQualifiedName())) continue;

						modifyEnumSourceInMethod(ast, rewrite, annotation, annBinding, enumValueName);
						modified[0]= true;
						break;
					}
				}
				return false;
			}
		});

		if (modified[0]) {
			applyChanges(cu, astRoot, rewrite, JUNIT5_ENUM_SOURCE_MODE_FQN);
		}
	}

	/**
	 * Replaces the {@code @EnumSource} annotation node with a new one that includes the given
	 * value in the {@code EXCLUDE} names list.
	 */
	private static void modifyEnumSourceInMethod(AST ast, ASTRewrite rewrite, Annotation annotation,
			IAnnotationBinding annBinding, String enumValueName) {

		// Read current state from binding
		String currentMode= null;
		List<String> currentNames= new java.util.ArrayList<>();
		org.eclipse.jdt.core.dom.Expression valueExpr= null;

		// Extract existing "value" expression from annotation AST node for copying
		if (annotation instanceof SingleMemberAnnotation) {
			valueExpr= ((SingleMemberAnnotation) annotation).getValue();
		} else if (annotation instanceof NormalAnnotation) {
			for (Object o : ((NormalAnnotation) annotation).values()) {
				MemberValuePair mvp= (MemberValuePair) o;
				if ("value".equals(mvp.getName().getIdentifier())) { //$NON-NLS-1$
					valueExpr= mvp.getValue();
				}
			}
		}

		// Read mode and names from binding (resolved)
		for (IMemberValuePairBinding pair : annBinding.getDeclaredMemberValuePairs()) {
			if ("mode".equals(pair.getName())) { //$NON-NLS-1$
				Object val= pair.getValue();
				if (val instanceof IVariableBinding) {
					currentMode= ((IVariableBinding) val).getName();
				}
			} else if ("names".equals(pair.getName())) { //$NON-NLS-1$
				Object val= pair.getValue();
				if (val instanceof Object[]) {
					for (Object item : (Object[]) val) {
						if (item instanceof String) {
							currentNames.add((String) item);
						}
					}
				} else if (val instanceof String) {
					currentNames.add((String) val);
				}
			}
		}

		modifyAnnotationToExclude(ast, rewrite, annotation, valueExpr, currentMode, currentNames, enumValueName);
	}

	/**
	 * Builds and installs a replacement {@code @EnumSource} annotation that excludes the given
	 * enum constant name.
	 */
	private static void modifyAnnotationToExclude(AST ast, ASTRewrite rewrite, Annotation annotation,
			org.eclipse.jdt.core.dom.Expression valueExpr, String currentMode,
			List<String> currentNames, String enumValueName) {

		NormalAnnotation newAnnotation= ast.newNormalAnnotation();
		newAnnotation.setTypeName(ast.newName("EnumSource")); //$NON-NLS-1$

		// Preserve the "value" attribute if present
		if (valueExpr != null) {
			MemberValuePair valuePair= ast.newMemberValuePair();
			valuePair.setName(ast.newSimpleName("value")); //$NON-NLS-1$
			valuePair.setValue((org.eclipse.jdt.core.dom.Expression) rewrite.createCopyTarget(valueExpr));
			newAnnotation.values().add(valuePair);
		}

		// If already EXCLUDE, keep it; otherwise set to EXCLUDE
		boolean isAlreadyExclude= "EXCLUDE".equals(currentMode); //$NON-NLS-1$

		// mode = EnumSource.Mode.EXCLUDE
		MemberValuePair modePair= ast.newMemberValuePair();
		modePair.setName(ast.newSimpleName("mode")); //$NON-NLS-1$
		QualifiedName modeValue= ast.newQualifiedName(
				ast.newQualifiedName(ast.newSimpleName("EnumSource"), ast.newSimpleName("Mode")), //$NON-NLS-1$ //$NON-NLS-2$
				ast.newSimpleName("EXCLUDE")); //$NON-NLS-1$
		modePair.setValue(modeValue);
		newAnnotation.values().add(modePair);

		// names = { existing..., newValue }
		ArrayInitializer namesArray= ast.newArrayInitializer();

		if (isAlreadyExclude) {
			// Preserve all existing excluded names
			for (String existingName : currentNames) {
				StringLiteral sl= ast.newStringLiteral();
				sl.setLiteralValue(existingName);
				namesArray.expressions().add(sl);
			}
		}
		// Add the new exclusion
		StringLiteral newNameLiteral= ast.newStringLiteral();
		newNameLiteral.setLiteralValue(enumValueName);
		namesArray.expressions().add(newNameLiteral);

		MemberValuePair namesPair= ast.newMemberValuePair();
		namesPair.setName(ast.newSimpleName("names")); //$NON-NLS-1$
		namesPair.setValue(namesArray);
		newAnnotation.values().add(namesPair);

		rewrite.replace(annotation, newAnnotation, null);
	}

	private static void applyChangesWithImportRemoval(ICompilationUnit cu, CompilationUnit astRoot, ASTRewrite rewrite, Annotation removedAnnotationNode) {
		try {
			MultiTextEdit multiEdit= new MultiTextEdit();
			ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(astRoot, true);

			if (removedAnnotationNode != null) {
				ImportRemover importRemover= new ImportRemover(cu.getJavaProject(), astRoot);
				importRemover.registerRemovedNode(removedAnnotationNode);
				importRemover.applyRemoves(importRewrite);
			}

			TextEdit importEdit= importRewrite.rewriteImports(null);
			if (importEdit.hasChildren() || importEdit.getLength() != 0) {
				multiEdit.addChild(importEdit);
			}

			TextEdit rewriteEdit= rewrite.rewriteAST();
			if (rewriteEdit.hasChildren() || rewriteEdit.getLength() != 0) {
				multiEdit.addChild(rewriteEdit);
			}

			// Apply the combined edit
			if (multiEdit.hasChildren()) {
				cu.applyTextEdit(multiEdit, null);
				cu.save(null, true);
			}
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}
}
