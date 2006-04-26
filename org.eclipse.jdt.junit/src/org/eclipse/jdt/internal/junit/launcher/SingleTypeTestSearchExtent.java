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

/**
 * 
 */
package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;


public class SingleTypeTestSearchExtent implements ITestSearchExtent {
	private IType fType;

	public SingleTypeTestSearchExtent(IType type) {
		fType = type;
	}

	public IType[] find(ITestFinder finder) throws JavaModelException {
		if (finder.isTest(fType))
			return new IType[] { fType };
		return new IType[0];
	}
}