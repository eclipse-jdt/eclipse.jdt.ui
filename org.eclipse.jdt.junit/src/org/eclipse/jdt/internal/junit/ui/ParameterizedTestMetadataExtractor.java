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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

/**
 * Utility class that populates parameterized-test metadata on {@link TestCaseElement} instances
 * by parsing AST source.
 *
 * <p>Metadata is populated lazily (on demand) and cached on the {@link TestCaseElement}.
 * After the first call to {@link #populate(TestCaseElement)}, subsequent calls are no-ops.
 *
 * @since 3.15
 */
public class ParameterizedTestMetadataExtractor {

	private static final String JUNIT5_PARAMETERIZED_TEST= "org.junit.jupiter.params.ParameterizedTest"; //$NON-NLS-1$
	private static final String JUNIT5_ENUM_SOURCE= "org.junit.jupiter.params.provider.EnumSource"; //$NON-NLS-1$
	private static final String JUNIT5_VALUE_SOURCE= "org.junit.jupiter.params.provider.ValueSource"; //$NON-NLS-1$
	private static final String JUNIT5_METHOD_SOURCE= "org.junit.jupiter.params.provider.MethodSource"; //$NON-NLS-1$
	private static final String JUNIT5_CSV_SOURCE= "org.junit.jupiter.params.provider.CsvSource"; //$NON-NLS-1$

	/**
	 * Populates parameterized-test metadata on the given {@link TestCaseElement}.
	 *
	 * <p>Metadata is only populated if it has not been populated yet
	 * ({@code getParameterSourceType() == null}).
	 *
	 * @param testCaseElement the test case element to populate
	 */
	public static void populate(TestCaseElement testCaseElement) {
		if (testCaseElement.getParameterSourceType() != null) {
			return; // already populated
		}

		// Mark as "checked but not parameterized" by default
		testCaseElement.setParameterSourceType(""); //$NON-NLS-1$

		TestSuiteElement parent= testCaseElement.getParent();
		if (parent == null) {
			return;
		}

		IMethod method= TestMethodFinder.findMethodForParameterizedTest(parent);
		if (method == null) {
			return;
		}

		try {
			ICompilationUnit cu= method.getCompilationUnit();
			if (cu == null) {
				return;
			}

			ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
			parser.setSource(cu);
			parser.setResolveBindings(true);
			CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

			final String[] sourceType= { null };
			final String[] enumTypeName= { null };

			astRoot.accept(new ASTVisitor() {
				@Override
				public boolean visit(MethodDeclaration node) {
					if (!node.getName().getIdentifier().equals(method.getElementName())) {
						return false;
					}

					IMethodBinding mb= node.resolveBinding();
					if (mb == null) {
						return false;
					}

					boolean isParameterized= false;
					for (IAnnotationBinding ann : mb.getAnnotations()) {
						ITypeBinding annType= ann.getAnnotationType();
						if (annType == null) continue;
						String fqn= annType.getQualifiedName();
						if (JUNIT5_PARAMETERIZED_TEST.equals(fqn)) {
							isParameterized= true;
							break;
						}
					}

					if (!isParameterized) {
						return false;
					}

					for (IAnnotationBinding ann : mb.getAnnotations()) {
						ITypeBinding annType= ann.getAnnotationType();
						if (annType == null) continue;
						String fqn= annType.getQualifiedName();
						if (JUNIT5_ENUM_SOURCE.equals(fqn)) {
							sourceType[0]= "EnumSource"; //$NON-NLS-1$
							// Try to get the declared enum type
							for (IMemberValuePairBinding pair : ann.getDeclaredMemberValuePairs()) {
								if ("value".equals(pair.getName())) { //$NON-NLS-1$
									Object val= pair.getValue();
									if (val instanceof ITypeBinding) {
										enumTypeName[0]= ((ITypeBinding) val).getQualifiedName();
									}
									break;
								}
							}
							// If not explicit, try first method parameter
							if (enumTypeName[0] == null && mb.getParameterTypes().length > 0) {
								ITypeBinding paramType= mb.getParameterTypes()[0];
								if (paramType != null && paramType.isEnum()) {
									enumTypeName[0]= paramType.getQualifiedName();
								}
							}
							break;
						} else if (JUNIT5_VALUE_SOURCE.equals(fqn)) {
							sourceType[0]= "ValueSource"; //$NON-NLS-1$
						} else if (JUNIT5_METHOD_SOURCE.equals(fqn)) {
							sourceType[0]= "MethodSource"; //$NON-NLS-1$
						} else if (JUNIT5_CSV_SOURCE.equals(fqn)) {
							sourceType[0]= "CsvSource"; //$NON-NLS-1$
						}
					}

					return false;
				}
			});

			if (sourceType[0] != null) {
				testCaseElement.setParameterizedTest(true);
				testCaseElement.setParameterSourceType(sourceType[0]);
				testCaseElement.setParameterEnumType(enumTypeName[0]);
			}

		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	private ParameterizedTestMetadataExtractor() {
		// Utility class - no instances
	}
}
