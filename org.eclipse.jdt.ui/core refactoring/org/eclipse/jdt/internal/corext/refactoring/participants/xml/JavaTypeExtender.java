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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class JavaTypeExtender extends PropertyTester {

	private static final String PROPERTY_HAS_MAIN_TYPE= "hasMainType"; //$NON-NLS-1$
	private static final String IS_ANONYMOUES= "isAnonymous";  //$NON-NLS-1$
	private static final String IS_LOCAL= "isLocal"; //$NON-NLS-1$
	
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IType type= (IType)receiver;
		if (PROPERTY_HAS_MAIN_TYPE.equals(method)) { //$NON-NLS-1$
			try {
				return JavaModelUtil.hasMainMethod(type);
			} catch (JavaModelException e) {
				return false;
			}
		} else if (IS_ANONYMOUES.equals(method)) {
			try {
				return type.isAnonymous();
			} catch (JavaModelException e) {
				return false;
			}
		} else if (IS_LOCAL.equals(method)) {
			try {
				return type.isLocal();
			} catch (JavaModelException e) {
				return false;
			}
		}
		Assert.isTrue(false);
		return false;
	}
}
