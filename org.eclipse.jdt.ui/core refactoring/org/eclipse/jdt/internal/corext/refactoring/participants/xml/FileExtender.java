/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;

public class FileExtender extends PropertyTester {

	private static final String PROPERTY_IS_CU= "isCompilationUnit"; //$NON-NLS-1$
	
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IFile file= (IFile)receiver;
		if (PROPERTY_IS_CU.equals(method)) {
			IJavaElement jElement= JavaCore.create(file);
			return jElement != null && jElement.exists() && jElement.getElementType() == IJavaElement.COMPILATION_UNIT;
		}
		Assert.isTrue(false);
		return false;
	}
}
