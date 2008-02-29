/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

public class JUnit3TestFinder implements ITestFinder {
	
	public void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		pm.beginTask(JUnitMessages.TestSearchEngine_message_searching, 10);
		try {
			if (element instanceof IType) {
				if (isTest((IType) element)) {
					result.add(element);
				}
			} else if (element instanceof ICompilationUnit) {
				IType[] types= ((ICompilationUnit) element).getAllTypes();
				for (int i= 0; i < types.length; i++) {
					IType type= types[i];
					if (isTest(types[i])) {
						result.add(type);
					}
				}
			} else {
				findTestCases(element, result, new SubProgressMonitor(pm, 7));
				if (pm.isCanceled()) {
					return;
				}
				TestSearchEngine.findSuiteMethods(element, result, new SubProgressMonitor(pm, 3));
			}
			if (pm.isCanceled()) {
				return;
			}
		} finally {
			pm.done();
		}
	}

	private static void findTestCases(IJavaElement element, Set result, IProgressMonitor pm) throws JavaModelException {
		IJavaProject javaProject= element.getJavaProject();

		IType testCaseType= javaProject.findType(JUnitPlugin.TEST_INTERFACE_NAME);
		if (testCaseType == null)
			return;

		IRegion region= TestSearchEngine.getRegion(element);
		ITypeHierarchy typeHierarchy= javaProject.newTypeHierarchy(testCaseType, region, pm);
		TestSearchEngine.findTestImplementorClasses(typeHierarchy, testCaseType, region, result);
	}

	public boolean isTest(IType type) throws JavaModelException {
		return TestSearchEngine.isAccessibleClass(type) && (TestSearchEngine.hasSuiteMethod(type) || isTestImplementor(type));
	}
	
	private static boolean isTestImplementor(IType type) throws JavaModelException {
		if (!Flags.isAbstract(type.getFlags()) && TestSearchEngine.isTestImplementor(type)) {
			return true;
		}
		return false;
	}
}
