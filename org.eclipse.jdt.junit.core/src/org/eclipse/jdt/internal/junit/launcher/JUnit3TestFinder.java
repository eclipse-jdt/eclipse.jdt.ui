/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.launcher;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;

public class JUnit3TestFinder implements ITestFinder {

	@Override
	public void findTestsInContainer(IJavaElement element, Set<IType> result, IProgressMonitor pm) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}

		SubMonitor subMon= SubMonitor.convert(pm, JUnitMessages.TestSearchEngine_message_searching, 10);
		try {
			if (element instanceof IType) {
				if (isTest((IType) element)) {
					result.add((IType) element);
				}
			} else if (element instanceof ICompilationUnit) {
				IType[] types= ((ICompilationUnit) element).getAllTypes();
				for (IType type : types) {
					if (isTest(type)) {
						result.add(type);
					}
				}
			} else {
				findTestCases(element, result, subMon.newChild(7));
				if (subMon.isCanceled()) {
					return;
				}
				CoreTestSearchEngine.findSuiteMethods(element, result, subMon.newChild(3));
			}
			if (subMon.isCanceled()) {
				return;
			}
		} finally {
			subMon.done();
		}
	}

	private static void findTestCases(IJavaElement element, Set<IType> result, IProgressMonitor pm) throws JavaModelException {
		IJavaProject javaProject= element.getJavaProject();

		IType testCaseType= javaProject.findType(JUnitCorePlugin.TEST_INTERFACE_NAME);
		if (testCaseType == null)
			return;

		IRegion region= CoreTestSearchEngine.getRegion(element);
		ITypeHierarchy typeHierarchy= javaProject.newTypeHierarchy(testCaseType, region, pm);
		CoreTestSearchEngine.findTestImplementorClasses(typeHierarchy, testCaseType, region, result);
	}

	@Override
	public boolean isTest(IType type) throws JavaModelException {
		return CoreTestSearchEngine.isAccessibleClass(type) && (CoreTestSearchEngine.hasSuiteMethod(type) || isTestImplementor(type));
	}

	private static boolean isTestImplementor(IType type) throws JavaModelException {
		if (!Flags.isAbstract(type.getFlags()) && CoreTestSearchEngine.isTestImplementor(type)) {
			return true;
		}
		return false;
	}
}
