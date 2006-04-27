/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Interface to be implemented by for extension point
 * org.eclipse.jdt.junit.internal_testKinds.
 */
public interface ITestFinder {
	ITestFinder NULL= new ITestFinder() {
		public void findTestsInContainer(Object[] elements, Set result, IProgressMonitor pm) {
			// do nothing
		}

		public boolean isTest(IType type) throws JavaModelException {
			return false;
		}
	};

	/**
	 * @param elements elements to search for tests
	 * @param result a Set to add ITypes
	 * @param pm
	 */
	public abstract void findTestsInContainer(Object[] elements, Set/*<IType>*/ result, IProgressMonitor pm);

	public abstract boolean isTest(IType type) throws JavaModelException;
}
