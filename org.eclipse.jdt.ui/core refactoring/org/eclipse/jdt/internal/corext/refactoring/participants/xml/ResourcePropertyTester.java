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

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.ui.util.StringMatcher;


public class ResourcePropertyTester implements IPropertyTester {

	private static final String PROPERTY_NAME= "name";	 //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public int test(Object element, String propertyName, String value) {
		if (!(element instanceof IResource))
			return UNKNOWN;
		IResource resource= (IResource)element;
		if (PROPERTY_NAME.equals(propertyName)) { //$NON-NLS-1$
			String fileName= resource.getName();
			StringMatcher matcher= new StringMatcher(value, false, false);
			return convert(matcher.match(fileName));
		}
		return UNKNOWN;
	}
	
	private int convert(boolean value) {
		return value ? TRUE : FALSE;
	}
}
