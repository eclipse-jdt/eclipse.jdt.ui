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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.ui.util.StringMatcher;


public class ResourcePropertyTester extends PropertyTester {

	private static final String PROPERTY_NAME= "name";	 //$NON-NLS-1$
	private static final String PROJECT_NATURE = "projectNature";	 //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public int test(Object element, String propertyName, String value) {
		IResource resource= (IResource)element;
		if (PROPERTY_NAME.equals(propertyName)) { //$NON-NLS-1$
			String fileName= resource.getName();
			StringMatcher matcher= new StringMatcher(value, false, false);
			return convert(matcher.match(fileName));
		} else if (PROJECT_NATURE.equals(propertyName)) {
			try {
				IProject proj = resource.getProject();
				return convert(proj.isAccessible() && proj.hasNature(value));
			} catch (CoreException e) {
				return FALSE;		
			}
		}
		return UNKNOWN;
	}
}
