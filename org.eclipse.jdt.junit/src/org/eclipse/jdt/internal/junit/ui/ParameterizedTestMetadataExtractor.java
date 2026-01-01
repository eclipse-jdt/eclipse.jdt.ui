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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;

/**
 * Utility class to extract and populate parameterized test metadata from source code.
 *
 * @since 3.15
 */
public class ParameterizedTestMetadataExtractor {

	/**
	 * Populates the parameterized test metadata for a TestCaseElement by examining its source code.
	 *
	 * @param testCase the test case element to populate
	 */
	public static void populateMetadata(TestCaseElement testCase) {
		if (testCase == null) {
			return;
		}

		try {
			String className= testCase.getTestClassName();
			String methodName= testCase.getTestMethodName();

			if (className == null || methodName == null) {
				return;
			}

			IType type= testCase.getTestRunSession().getLaunchedProject().findType(className);
			if (type == null) {
				return;
			}

			IMethod method= findTestMethod(type, methodName);
			if (method == null) {
				return;
			}

			extractMetadata(method, testCase);
		} catch (Exception e) {
			// Silently fail - metadata is optional
		}
	}

	private static IMethod findTestMethod(IType type, String methodName) throws JavaModelException {
		IMethod[] methods= type.getMethods();
		for (IMethod method : methods) {
			if (method.getElementName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}

	private static void extractMetadata(IMethod method, TestCaseElement testCase) throws JavaModelException {
		ICompilationUnit cu= method.getCompilationUnit();
		if (cu == null) {
			return;
		}

		// Parse the compilation unit
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		// Find the method declaration
		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					analyzeMethodAnnotations(node, testCase);
					return false;
				}
				return true;
			}
		});
	}

	private static void analyzeMethodAnnotations(MethodDeclaration methodDecl, TestCaseElement testCase) {
		List<?> modifiers= methodDecl.modifiers();
		boolean hasParameterizedTest= false;
		String sourceType= null;
		String enumType= null;

		for (Object modifier : modifiers) {
			if (modifier instanceof org.eclipse.jdt.core.dom.Annotation) {
				org.eclipse.jdt.core.dom.Annotation annotation= (org.eclipse.jdt.core.dom.Annotation) modifier;
				String annotationName= annotation.getTypeName().getFullyQualifiedName();

				if ("ParameterizedTest".equals(annotationName) ||  //$NON-NLS-1$
					"org.junit.jupiter.params.ParameterizedTest".equals(annotationName)) { //$NON-NLS-1$
					hasParameterizedTest= true;
				}

				// Check for various source annotations
				if (annotationName.endsWith("EnumSource")) { //$NON-NLS-1$
					sourceType= "EnumSource"; //$NON-NLS-1$
					enumType= extractEnumType(annotation);
				} else if (annotationName.endsWith("ValueSource")) { //$NON-NLS-1$
					sourceType= "ValueSource"; //$NON-NLS-1$
				} else if (annotationName.endsWith("MethodSource")) { //$NON-NLS-1$
					sourceType= "MethodSource"; //$NON-NLS-1$
				} else if (annotationName.endsWith("CsvSource")) { //$NON-NLS-1$
					sourceType= "CsvSource"; //$NON-NLS-1$
				}
			}
		}

		// Populate the test case metadata
		if (hasParameterizedTest && sourceType != null) {
			testCase.setParameterizedTest(true);
			testCase.setParameterSourceType(sourceType);
			if (enumType != null) {
				testCase.setParameterEnumType(enumType);
			}
		}
	}

	private static String extractEnumType(org.eclipse.jdt.core.dom.Annotation annotation) {
		Expression valueExpr= null;

		if (annotation instanceof org.eclipse.jdt.core.dom.SingleMemberAnnotation) {
			valueExpr= ((org.eclipse.jdt.core.dom.SingleMemberAnnotation) annotation).getValue();
		} else if (annotation instanceof org.eclipse.jdt.core.dom.NormalAnnotation) {
			List<?> values= ((org.eclipse.jdt.core.dom.NormalAnnotation) annotation).values();
			for (Object obj : values) {
				if (obj instanceof org.eclipse.jdt.core.dom.MemberValuePair) {
					org.eclipse.jdt.core.dom.MemberValuePair pair= (org.eclipse.jdt.core.dom.MemberValuePair) obj;
					if ("value".equals(pair.getName().getIdentifier())) { //$NON-NLS-1$
						valueExpr= pair.getValue();
						break;
					}
				}
			}
		}

		if (valueExpr instanceof TypeLiteral) {
			TypeLiteral typeLiteral= (TypeLiteral) valueExpr;
			Type type= typeLiteral.getType();
			ITypeBinding binding= type.resolveBinding();
			if (binding != null) {
				return binding.getQualifiedName();
			}
		}

		return null;
	}
}
