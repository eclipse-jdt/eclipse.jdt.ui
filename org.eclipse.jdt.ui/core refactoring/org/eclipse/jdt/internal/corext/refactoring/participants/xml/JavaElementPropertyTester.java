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

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.refactoring.Checks;


public class JavaElementPropertyTester extends PropertyTester {

	private static final String PROPERTY_IS_AVAILABLE= "isAvailable"; //$NON-NLS-1$
	
	public int test(Object element, String propertyName, String value) throws CoreException {
		IJavaElement jElement= (IJavaElement)element;
		if (PROPERTY_IS_AVAILABLE.equals(propertyName)) {
			return testBoolean(value, Checks.isAvailable(jElement));
		}
		return UNKNOWN;
	}
}
