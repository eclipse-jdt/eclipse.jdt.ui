/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial tests
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator.ExclusionTarget;

/** Test fixtures and assertions using the same invocation resolver as the action. */
final class EnumSourceTestSupport {

	static TestCaseElement invocation(IMethod method, int index) throws JavaModelException {
		String[] signatures= method.getParameterTypes();
		String[] parameterTypes= new String[signatures.length];
		for (int i= 0; i < signatures.length; i++) {
			parameterTypes[i]= JavaModelUtil.getResolvedTypeName(signatures[i], method.getDeclaringType(), '$');
			assertNotNull(parameterTypes[i]);
		}
		String className= method.getDeclaringType().getFullyQualifiedName('$');
		String methodName= method.getElementName() + '(' + String.join(",", parameterTypes) + ')'; //$NON-NLS-1$
		String uniqueId= "[engine:junit-jupiter]/[class:%s]/[test-template:%s]/[test-template-invocation:#%d]" //$NON-NLS-1$
				.formatted(className, methodName, index);
		TestRunSession session= new TestRunSession("EnumSource test", method.getJavaProject()); //$NON-NLS-1$
		TestSuiteElement suite= new TestSuiteElement(session.getTestRoot(), "suite", //$NON-NLS-1$
				methodName + '(' + className + ')', 1, null, parameterTypes, null);
		return new TestCaseElement(suite, "case", "custom(" + className + ')', //$NON-NLS-1$ //$NON-NLS-2$
				"custom display", true, null, uniqueId); //$NON-NLS-1$
	}

	static String enumConstantForInvocation(IMethod method, int index) throws JavaModelException {
		ExclusionTarget target= EnumSourceValidator.findExclusionTarget(invocation(method, index));
		return target == null ? null : target.enumConstantName();
	}

	static void assertExcludeMode(IMethod method) throws JavaModelException {
		IMemberValuePair mode= member(method, "mode"); //$NON-NLS-1$
		assertNotNull(mode);
		assertTrue(mode.getValue() instanceof String);
		String value= (String) mode.getValue();
		assertTrue(value.equals("EXCLUDE") || value.endsWith(".EXCLUDE")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	static void assertFilterRemoved(IMethod method) throws JavaModelException {
		assertNull(member(method, "mode")); //$NON-NLS-1$
		assertNull(member(method, "names")); //$NON-NLS-1$
	}

	private static IMemberValuePair member(IMethod method, String name) throws JavaModelException {
		for (IAnnotation annotation : method.getAnnotations()) {
			String type= annotation.getElementName();
			if (type.equals("EnumSource") || type.equals("org.junit.jupiter.params.provider.EnumSource")) { //$NON-NLS-1$ //$NON-NLS-2$
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if (name.equals(pair.getMemberName())) {
						return pair;
					}
				}
				return null;
			}
		}
		throw new AssertionError("Missing @EnumSource on " + method.getElementName()); //$NON-NLS-1$
	}

	private EnumSourceTestSupport() {
	}
}
