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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IMethod;

import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;

public class MethodPropertyTester extends PropertyTester {

	private static final String PROPERTY_IS_VIRTUAL= "isVirtual"; //$NON-NLS-1$
	private static final String PROPERTY_DECLARING_TYPE= "declaringType"; //$NON-NLS-1$
	private static final String PROPERTY_IS_CONSTRUCTOR= "isConstructor";  //$NON-NLS-1$
	private static final String PROPERTY_IS_MAIN_METHOD= "isMainMethod";  //$NON-NLS-1$
	
	public int test(Object element, String propertyName, String value) throws CoreException {
		IMethod method= (IMethod)element;
		if (PROPERTY_IS_VIRTUAL.equals(propertyName)) {
			return testBoolean(value, MethodChecks.isVirtual(method));
		} else if (PROPERTY_DECLARING_TYPE.equals(propertyName)) {
			convert(Expression.isInstanceOf(method, value));
		} else if (PROPERTY_IS_CONSTRUCTOR.equals(propertyName)) {
			return testBoolean(value, method.isConstructor());
		} else if (PROPERTY_IS_MAIN_METHOD.equals(propertyName)) {
			return testBoolean(value, method.isMainMethod());
		}
		return UNKNOWN;
	}
}
