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


public class TypePropertyTester extends TypeExtender {

	private static final String PROPERTY_HAS_MAIN_TYPE= "hasMainType"; //$NON-NLS-1$
	private static final String IS_ANONYMOUES= "isAnonymous";  //$NON-NLS-1$
	private static final String IS_LOCAL= "isLocal"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public Object perform(Object receiver, String method, Object[] args) {
		IType type= (IType)receiver;
		if (PROPERTY_HAS_MAIN_TYPE.equals(method)) { //$NON-NLS-1$
			try {
				return Boolean.valueOf(JavaModelUtil.hasMainMethod(type));
			} catch (JavaModelException e) {
				return Boolean.FALSE;
			}
		} else if (IS_ANONYMOUES.equals(method)) {
			try {
				return Boolean.valueOf(type.isAnonymous());
			} catch (JavaModelException e) {
				return Boolean.FALSE;
			}
		} else if (IS_LOCAL.equals(method)) {
			try {
				return Boolean.valueOf(type.isLocal());
			} catch (JavaModelException e) {
				return Boolean.FALSE;
			}
		}
		Assert.isTrue(false);
		return null;
	}
}
