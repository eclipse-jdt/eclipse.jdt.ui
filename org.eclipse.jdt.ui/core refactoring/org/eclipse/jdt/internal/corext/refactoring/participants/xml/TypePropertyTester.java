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

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class TypePropertyTester extends PropertyTester {

	private static final String PROPERTY_HAS_MAIN_TYPE= "hasMainType"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public int test(Object element, String propertyName, String value) {
		IType type= (IType)element;
		if (PROPERTY_HAS_MAIN_TYPE.equals(propertyName)) { //$NON-NLS-1$
			try {
				return testBoolean(value, JavaModelUtil.hasMainMethod(type));
			} catch (JavaModelException e) {
				return FALSE;
			}
		}
		return UNKNOWN;
	}
}
