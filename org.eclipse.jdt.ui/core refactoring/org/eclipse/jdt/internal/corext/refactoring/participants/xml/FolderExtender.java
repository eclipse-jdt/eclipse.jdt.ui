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

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;

public class FolderExtender extends PropertyTester {

	private static final String PROPERTY_IS_SOURCE_FOLDER= "isSourceFolder"; //$NON-NLS-1$
	
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IFolder folder= (IFolder)receiver;
		if (PROPERTY_IS_SOURCE_FOLDER.equals(method)) {
			IJavaElement jElement= JavaCore.create(folder);
			return jElement != null && jElement.exists() && jElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT;
		}
		Assert.isTrue(false);
		return false;
	}
}
