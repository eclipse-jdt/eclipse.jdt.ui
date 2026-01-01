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
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

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

	private static final String JUNIT4_IGNORE_ANNOTATION = "org.junit.Ignore"; //$NON-NLS-1$
	private static final String JUNIT5_DISABLED_ANNOTATION = "org.junit.jupiter.api.Disabled"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_ANNOTATION = "org.junit.jupiter.api.Test"; //$NON-NLS-1$
	private static final String JUNIT5_PARAMETERIZED_TEST_ANNOTATION = "org.junit.jupiter.params.ParameterizedTest"; //$NON-NLS-1$
	private static final String JUNIT5_REPEATED_TEST_ANNOTATION = "org.junit.jupiter.api.RepeatedTest"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_FACTORY_ANNOTATION = "org.junit.jupiter.api.TestFactory"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_TEMPLATE_ANNOTATION = "org.junit.jupiter.api.TestTemplate"; //$NON-NLS-1$

	/**
	 * Add @Disabled (JUnit 5) or @Ignore (JUnit 4) annotation to a method.
	 *
	 * @param method the method to add the annotation to
	 * @param isJUnit5 whether to use JUnit 5 (@Disabled) or JUnit 4 (@Ignore)
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void addDisabledAnnotation(IMethod method, boolean isJUnit5) throws JavaModelException {
		String annotationQualifiedName = isJUnit5 ? JUNIT5_DISABLED_ANNOTATION : JUNIT4_IGNORE_ANNOTATION;
		String annotationSimpleName = isJUnit5 ? "Disabled" : "Ignore"; //$NON-NLS-1$ //$NON-NLS-2$

		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		// Parse the compilation unit
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		AST ast = astRoot.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		final boolean[] modified = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					// Add the annotation
					org.eclipse.jdt.core.dom.MarkerAnnotation annotation = ast.newMarkerAnnotation();
					annotation.setTypeName(ast.newName(annotationSimpleName));

					ListRewrite listRewrite = rewrite.getListRewrite(node, MethodDeclaration.MODIFIERS2_PROPERTY);
					listRewrite.insertFirst(annotation, null);
					modified[0] = true;
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
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		// Parse the compilation unit
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		AST ast = astRoot.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		final boolean[] modified = new boolean[] { false };
		final String[] removedAnnotation = new String[1];

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					IMethodBinding methodBinding = node.resolveBinding();
					if (methodBinding != null) {
						IAnnotationBinding[] annotations = methodBinding.getAnnotations();
						for (IAnnotationBinding annotationBinding : annotations) {
							ITypeBinding annotationType = annotationBinding.getAnnotationType();
							if (annotationType != null) {
								String qualifiedName = annotationType.getQualifiedName();
								if (JUNIT5_DISABLED_ANNOTATION.equals(qualifiedName) ||
									JUNIT4_IGNORE_ANNOTATION.equals(qualifiedName)) {
									// Find and remove the annotation
									List<?> modifiers = node.modifiers();
									for (Object modifier : modifiers) {
										if (modifier instanceof Annotation) {
											Annotation annotation = (Annotation) modifier;
											if (annotation.resolveAnnotationBinding() == annotationBinding) {
												ListRewrite listRewrite = rewrite.getListRewrite(node, MethodDeclaration.MODIFIERS2_PROPERTY);
												listRewrite.remove(annotation, null);
												removedAnnotation[0] = qualifiedName;
												modified[0] = true;
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
			applyChangesWithImportRemoval(cu, astRoot, rewrite, removedAnnotation[0]);
		}
	}

	/**
	 * Modify @EnumSource annotation to exclude a specific value.
	 *
	 * @param method the method with the @EnumSource annotation
	 * @param enumValue the enum value to exclude
	 * @throws JavaModelException if there's an error accessing the Java model
	 */
	public static void excludeEnumValue(IMethod method, String enumValue) throws JavaModelException {
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		// Parse the compilation unit
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
		final boolean[] modified = new boolean[] { false };
		final boolean[] needsModeImport = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					needsModeImport[0] = modifyEnumSourceInMethod(node, enumValue, rewrite);
					modified[0] = needsModeImport[0];
				}
				return false;
			}
		});

		if (modified[0]) {
			// Add Mode import when we add mode=EXCLUDE
			String importToAdd = needsModeImport[0] ? "org.junit.jupiter.params.provider.EnumSource.Mode" : null; //$NON-NLS-1$
			applyChanges(cu, astRoot, rewrite, importToAdd);
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
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return false;
		}

		// Parse the compilation unit
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		final boolean[] result = new boolean[] { false };

		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					IMethodBinding methodBinding = node.resolveBinding();
					if (methodBinding != null) {
						IAnnotationBinding[] annotations = methodBinding.getAnnotations();
						for (IAnnotationBinding annotationBinding : annotations) {
							ITypeBinding annotationType = annotationBinding.getAnnotationType();
							if (annotationType != null) {
								String qualifiedName = annotationType.getQualifiedName();
								if (JUNIT5_DISABLED_ANNOTATION.equals(qualifiedName) ||
									JUNIT4_IGNORE_ANNOTATION.equals(qualifiedName)) {
									result[0] = true;
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
		IMethodBinding binding = methodDecl.resolveBinding();
		if (binding == null) {
			return false;
		}

		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			ITypeBinding annotationType = annotation.getAnnotationType();
			if (annotationType != null) {
				String qualifiedName = annotationType.getQualifiedName();
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
		IMethodBinding binding = methodDecl.resolveBinding();
		if (binding == null) {
			return false;
		}

		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			ITypeBinding annotationType = annotation.getAnnotationType();
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

	private static boolean modifyEnumSourceInMethod(MethodDeclaration methodDecl, String paramValue, ASTRewrite rewrite) {
		// Find @EnumSource annotation
		List<?> modifiers = methodDecl.modifiers();
		for (Object modifier : modifiers) {
			if (modifier instanceof Annotation) {
				Annotation annotation = (Annotation) modifier;
				String annotationName = annotation.getTypeName().getFullyQualifiedName();

				if ("EnumSource".equals(annotationName) ||  //$NON-NLS-1$
					"org.junit.jupiter.params.provider.EnumSource".equals(annotationName)) { //$NON-NLS-1$
					// Modify this annotation to add the exclusion
					modifyAnnotationToExclude(annotation, paramValue, rewrite);
					return true;
				}
			}
		}
		return false;
	}

	private static void modifyAnnotationToExclude(Annotation annotation, String paramValue, ASTRewrite rewrite) {
		AST ast = annotation.getAST();

		// Check if annotation already has names and mode attributes
		Expression valueExpr = null;
		MemberValuePair existingModePair = null;
		MemberValuePair existingNamesPair = null;
		List<String> existingNames = new ArrayList<>();
		boolean isExcludeMode = false;

		if (annotation instanceof org.eclipse.jdt.core.dom.NormalAnnotation) {
			org.eclipse.jdt.core.dom.NormalAnnotation normalAnnotation = (org.eclipse.jdt.core.dom.NormalAnnotation) annotation;
			List<?> values = normalAnnotation.values();
			for (Object obj : values) {
				if (obj instanceof MemberValuePair) {
					MemberValuePair pair = (MemberValuePair) obj;
					String name = pair.getName().getIdentifier();
					if ("value".equals(name)) { //$NON-NLS-1$
						valueExpr = pair.getValue();
					} else if ("mode".equals(name)) { //$NON-NLS-1$
						existingModePair = pair;
						// Check if it's EXCLUDE mode
						Expression modeValue = pair.getValue();
						if (modeValue instanceof QualifiedName) {
							QualifiedName qn = (QualifiedName) modeValue;
							isExcludeMode = "EXCLUDE".equals(qn.getName().getIdentifier()); //$NON-NLS-1$
						}
					} else if ("names".equals(name)) { //$NON-NLS-1$
						existingNamesPair = pair;
						// Extract existing names
						Expression namesValue = pair.getValue();
						if (namesValue instanceof org.eclipse.jdt.core.dom.ArrayInitializer) {
							org.eclipse.jdt.core.dom.ArrayInitializer array = (org.eclipse.jdt.core.dom.ArrayInitializer) namesValue;
							List<?> expressions = array.expressions();
							for (Object expr : expressions) {
								if (expr instanceof StringLiteral) {
									StringLiteral literal = (StringLiteral) expr;
									existingNames.add(literal.getLiteralValue());
								}
							}
						}
					}
				}
			}
		} else if (annotation instanceof org.eclipse.jdt.core.dom.SingleMemberAnnotation) {
			valueExpr = ((org.eclipse.jdt.core.dom.SingleMemberAnnotation) annotation).getValue();
		}

		// Create new NormalAnnotation
		org.eclipse.jdt.core.dom.NormalAnnotation newAnnotation = ast.newNormalAnnotation();
		newAnnotation.setTypeName(ast.newName(annotation.getTypeName().getFullyQualifiedName()));

		// Copy value attribute
		if (valueExpr != null) {
			MemberValuePair valuePair = ast.newMemberValuePair();
			valuePair.setName(ast.newSimpleName("value")); //$NON-NLS-1$
			valuePair.setValue((Expression) rewrite.createCopyTarget(valueExpr));
			newAnnotation.values().add(valuePair);
		}

		// Determine the new names list based on existing mode
		List<String> newNames = new ArrayList<>();

		if (existingNamesPair == null) {
			// No existing names attribute: Add mode=EXCLUDE and names={paramValue}
			newNames.add(paramValue);
			isExcludeMode = true;
		} else if (isExcludeMode) {
			// Existing EXCLUDE mode: Add paramValue to the list if not already present
			newNames.addAll(existingNames);
			if (!newNames.contains(paramValue)) {
				newNames.add(paramValue);
			}
		} else {
			// Existing INCLUDE mode: Remove paramValue from the list
			for (String name : existingNames) {
				if (!name.equals(paramValue)) {
					newNames.add(name);
				}
			}
			// If we removed the last value, switch to EXCLUDE mode with just this value
			if (newNames.isEmpty()) {
				newNames.add(paramValue);
				isExcludeMode = true;
			}
		}

		// Add mode parameter if EXCLUDE mode
		if (isExcludeMode) {
			MemberValuePair modePair = ast.newMemberValuePair();
			modePair.setName(ast.newSimpleName("mode")); //$NON-NLS-1$
			// Use simple qualified name: Mode.EXCLUDE (import will be added)
			QualifiedName modeName = ast.newQualifiedName(
				ast.newSimpleName("Mode"), //$NON-NLS-1$
				ast.newSimpleName("EXCLUDE") //$NON-NLS-1$
			);
			modePair.setValue(modeName);
			newAnnotation.values().add(modePair);
		}

		// Add names parameter with the new list
		MemberValuePair namesPair = ast.newMemberValuePair();
		namesPair.setName(ast.newSimpleName("names")); //$NON-NLS-1$
		org.eclipse.jdt.core.dom.ArrayInitializer arrayInit = ast.newArrayInitializer();
		for (String name : newNames) {
			StringLiteral literal = ast.newStringLiteral();
			literal.setLiteralValue(name);
			arrayInit.expressions().add(literal);
		}
		namesPair.setValue(arrayInit);
		newAnnotation.values().add(namesPair);

		// Replace old annotation with new one
		rewrite.replace(annotation, newAnnotation, null);
	}

	private static void applyChanges(ICompilationUnit cu, CompilationUnit astRoot, ASTRewrite rewrite, String annotationToImport) throws JavaModelException {
		try {
			MultiTextEdit multiEdit = new MultiTextEdit();

			// Add import if needed
			if (annotationToImport != null) {
				ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
				importRewrite.addImport(annotationToImport);

				TextEdit importEdit = importRewrite.rewriteImports(null);
				if (importEdit.hasChildren() || importEdit.getLength() != 0) {
					multiEdit.addChild(importEdit);
				}
			}

			TextEdit rewriteEdit = rewrite.rewriteAST();
			multiEdit.addChild(rewriteEdit);

			// Apply the combined edit
			cu.applyTextEdit(multiEdit, null);
			cu.save(null, true);
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	private static void applyChangesWithImportRemoval(ICompilationUnit cu, CompilationUnit astRoot, ASTRewrite rewrite, String annotationToRemove) throws JavaModelException {
		try {
			MultiTextEdit multiEdit = new MultiTextEdit();

			// Remove import if needed
			if (annotationToRemove != null) {
				ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
				importRewrite.removeImport(annotationToRemove);

				TextEdit importEdit = importRewrite.rewriteImports(null);
				if (importEdit.hasChildren() || importEdit.getLength() != 0) {
					multiEdit.addChild(importEdit);
				}
			}

			TextEdit rewriteEdit = rewrite.rewriteAST();
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
