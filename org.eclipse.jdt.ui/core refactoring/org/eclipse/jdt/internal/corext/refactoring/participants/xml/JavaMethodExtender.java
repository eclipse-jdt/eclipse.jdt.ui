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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;

public class JavaMethodExtender extends PropertyTester {

	private static final String PROPERTY_IS_VIRTUAL= "isVirtual"; //$NON-NLS-1$
	private static final String PROPERTY_IS_CONSTRUCTOR= "isConstructor";  //$NON-NLS-1$
	private static final String PROPERTY_IS_MAIN_METHOD= "isMainMethod";  //$NON-NLS-1$
	
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IMethod jMethod= (IMethod)receiver;
		try {
			if (PROPERTY_IS_VIRTUAL.equals(method)) {
				return MethodChecks.isVirtual(jMethod);
			} else if (PROPERTY_IS_CONSTRUCTOR.equals(method)) {
				return jMethod.isConstructor();
			} else if (PROPERTY_IS_MAIN_METHOD.equals(method)) {
				return jMethod.isMainMethod();
			}
		} catch (JavaModelException e) {
			return false;
		}
		Assert.isTrue(false);
		return false;
	}
}
