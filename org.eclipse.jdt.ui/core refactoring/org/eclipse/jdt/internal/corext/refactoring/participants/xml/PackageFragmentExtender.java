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

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.Assert;

public class PackageFragmentExtender extends PropertyTester {

	private static final String IS_DEFAULT_PACKAGE= "isDefaultPackage"; //$NON-NLS-1$
	
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IPackageFragment fragement= (IPackageFragment)receiver;
		if (IS_DEFAULT_PACKAGE.equals(method)) { 
			return fragement.isDefaultPackage();
		}
		Assert.isTrue(false);
		return false;
	}
}
