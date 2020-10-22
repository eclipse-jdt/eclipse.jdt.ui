/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.ui.unittest.junit.launcher;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;

class JUnitTestKindUtil {
	public static final String JUNIT3_TEST_KIND_ID = "org.eclipse.jdt.ui.unittest.junit.loader.junit3"; //$NON-NLS-1$
	public static final String JUNIT4_TEST_KIND_ID = "org.eclipse.jdt.ui.unittest.junit.loader.junit4"; //$NON-NLS-1$
	public static final String JUNIT5_TEST_KIND_ID = "org.eclipse.jdt.ui.unittest.junit.loader.junit5"; //$NON-NLS-1$

	private JUnitTestKindUtil() {
	}

	public static String getContainerTestKindId(IJavaElement element) {
		if (element != null) {
			IJavaProject project = element.getJavaProject();
			if (CoreTestSearchEngine.is50OrHigher(project)) {
				if (CoreTestSearchEngine.is18OrHigher(project)) {
					if (isRunWithJUnitPlatform(element)) {
						return JUNIT4_TEST_KIND_ID;
					}
					if (CoreTestSearchEngine.hasJUnit5TestAnnotation(project)) {
						return JUNIT5_TEST_KIND_ID;
					}
				}
				if (CoreTestSearchEngine.hasJUnit4TestAnnotation(project)) {
					return JUNIT4_TEST_KIND_ID;
				}
			}
		}
		return JUNIT3_TEST_KIND_ID;
	}

	/**
	 * @param element the element
	 * @return <code>true</code> if the element is a test class annotated with
	 *         <code>@RunWith(JUnitPlatform.class)</code>
	 */
	public static boolean isRunWithJUnitPlatform(IJavaElement element) {
		if (element instanceof ICompilationUnit) {
			element = ((ICompilationUnit) element).findPrimaryType();
		}
		if (element instanceof IType) {
			IType type = (IType) element;
			try {
				IAnnotation runWithAnnotation = type.getAnnotation("RunWith"); //$NON-NLS-1$
				if (!runWithAnnotation.exists()) {
					runWithAnnotation = type.getAnnotation("org.junit.runner.RunWith"); //$NON-NLS-1$
				}
				if (runWithAnnotation.exists()) {
					IMemberValuePair[] memberValuePairs = runWithAnnotation.getMemberValuePairs();
					for (IMemberValuePair memberValuePair : memberValuePairs) {
						if (memberValuePair.getMemberName().equals("value") //$NON-NLS-1$
								&& memberValuePair.getValue().equals("JUnitPlatform")) { //$NON-NLS-1$
							return true;
						}
					}
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		return false;
	}
}
