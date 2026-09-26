/*******************************************************************************
 * Copyright (c) 2025, 2026 Carsten Hammer and others.
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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

/**
 * Resolves the Java method represented by a parameterized-test suite node.
 *
 */
public final class TestMethodFinder {

	private static final char PARAM_START= '(';

	/**
	 * Finds the method represented by a parameterized-test suite.
	 *
	 * <p>When JUnit supplied parameter types, they are used to resolve overloads.
	 * Without parameter metadata, a method is returned only if its name is unique.</p>
	 *
	 * @param testSuiteElement the test suite element
	 * @return the method, or <code>null</code> if it cannot be resolved unambiguously
	 */
	public static IMethod findMethodForParameterizedTest(TestSuiteElement testSuiteElement) {
		if (testSuiteElement == null) {
			return null;
		}

		String testName= testSuiteElement.getTestName();
		int index= testName.indexOf(PARAM_START);
		if (index < 0) {
			return null;
		}

		String className= testSuiteElement.getSuiteTypeName();
		if (className == null || className.isEmpty()) {
			return null;
		}

		IJavaProject javaProject= testSuiteElement.getTestRunSession().getLaunchedProject();
		if (javaProject == null) {
			return null;
		}

		try {
			IType type= javaProject.findType(className);
			if (type == null) {
				return null;
			}
			return findMethod(type, testName.substring(0, index), testSuiteElement.getParameterTypes());
		} catch (JavaModelException | IllegalArgumentException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}

	/**
	 * Resolves a method by name and optional fully qualified parameter type names.
	 *
	 * @param type the declaring type
	 * @param methodName the method name
	 * @param parameterTypes parameter type names supplied by JUnit, or <code>null</code>
	 * @return the method, or <code>null</code> if resolution is ambiguous
	 * @throws JavaModelException if the Java model cannot be read
	 */
	public static IMethod findMethod(IType type, String methodName, String[] parameterTypes)
			throws JavaModelException {
		IMethod result= null;
		for (IMethod method : type.getMethods()) {
			if (!methodName.equals(method.getElementName())
					|| parameterTypes != null && !hasParameterTypes(method, parameterTypes)) {
				continue;
			}
			if (result != null) {
				return null;
			}
			result= method;
		}
		return result;
	}

	private static boolean hasParameterTypes(IMethod method, String[] expectedParameterTypes)
			throws JavaModelException {
		String[] parameterTypes= method.getParameterTypes();
		if (parameterTypes.length != expectedParameterTypes.length) {
			return false;
		}

		IType declaringType= method.getDeclaringType();
		for (int i= 0; i < parameterTypes.length; i++) {
			String expectedType= normalizeParameterType(expectedParameterTypes[i]);
			if (expectedType == null) {
				return false;
			}

			String erasedType= Signature.getTypeErasure(parameterTypes[i]);
			String binaryType= resolveTypeName(erasedType, declaringType, '$');
			if (expectedType.equals(binaryType)) {
				continue;
			}

			String sourceType= resolveTypeName(erasedType, declaringType, '.');
			if (!expectedType.equals(sourceType)) {
				return false;
			}
		}
		return true;
	}

	private static String resolveTypeName(String typeSignature, IType declaringType,
			char enclosingTypeSeparator) throws JavaModelException {
		int dimensions= Signature.getArrayCount(typeSignature);
		String elementTypeSignature= Signature.getElementType(typeSignature);
		String resolvedTypeName;
		if (Signature.getTypeSignatureKind(elementTypeSignature) == Signature.BASE_TYPE_SIGNATURE) {
			resolvedTypeName= Signature.toString(elementTypeSignature);
		} else {
			resolvedTypeName= JavaModelUtil.getResolvedTypeName(elementTypeSignature, declaringType,
					enclosingTypeSeparator);
		}
		if (resolvedTypeName == null) {
			return null;
		}

		StringBuilder result= new StringBuilder(resolvedTypeName);
		for (int i= 0; i < dimensions; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}

	private static String normalizeParameterType(String parameterType) {
		if (parameterType == null) {
			return null;
		}
		String result= parameterType.trim();
		if (result.isEmpty()) {
			return null;
		}
		if (result.charAt(0) == Signature.C_ARRAY) {
			try {
				return Signature.toString(result.replace('/', '.'));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		if (result.endsWith("...")) { //$NON-NLS-1$
			result= result.substring(0, result.length() - 3) + "[]"; //$NON-NLS-1$
		}
		return result;
	}

	private TestMethodFinder() {
		// Utility class - no instances
	}
}
