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

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.Assert;

public class PackageFragmentExtender extends TypeExtender {

	private static final String IS_DEFAULT_PACKAGE= "isDefaultPackage"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public Object invoke(Object receiver, String method, Object[] args) {
		IPackageFragment fragement= (IPackageFragment)receiver;
		if (IS_DEFAULT_PACKAGE.equals(method)) { 
			return Boolean.valueOf(fragement.isDefaultPackage());
		}
		Assert.isTrue(false);
		return null;
	}
}
