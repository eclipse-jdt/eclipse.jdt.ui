/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;


import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

public class JUnit3TestFinder implements ITestFinder {
	public void findTestsInContainer(IJavaElement container, Set result, IProgressMonitor pm) {
		try {
			TestSearchEngine.doFindTests(new Object[] { container }, result, pm);
		} catch (InterruptedException e) {
		}
	}

	public boolean isTest(IType type) throws JavaModelException {
		return hasSuiteMethod(type) || isTestType(type);
	}

	protected boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]); //$NON-NLS-1$
		if (method == null || !method.exists())
			return false;

		if (!Flags.isStatic(method.getFlags()) || !Flags.isPublic(method.getFlags()) || !Flags.isPublic(method.getDeclaringType().getFlags())) {
			return false;
		}
		if (!Signature.getSimpleName(Signature.toString(method.getReturnType())).equals(JUnitPlugin.SIMPLE_TEST_INTERFACE_NAME)) {
			return false;
		}
		return true;
	}

	protected boolean isTestType(IType type) throws JavaModelException {
		if (!TestSearchEngine.hasValidModifiers(type))
			return false;

		IType[] interfaces= type.newSupertypeHierarchy(null).getAllSuperInterfaces(type);
		for (int i= 0; i < interfaces.length; i++)
			if (interfaces[i].getFullyQualifiedName('.').equals(JUnitPlugin.TEST_INTERFACE_NAME))
				return true;
		return false;
	}
}
