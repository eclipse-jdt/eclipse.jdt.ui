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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.util.StringMatcher;


public class PropertyExpression extends Expression {

	private String fName;
	private String fValue;
	
	public static final String NAME= "property"; //$NON-NLS-1$
	private static final String ATT_NAME= "name"; //$NON-NLS-1$
	
	private static final String PROPERTY_NAME= "name";	 //$NON-NLS-1$
	private static final String PROPERTY_INSTANCE_OF= "instanceOf";	 //$NON-NLS-1$
	private static final String PROPERTY_HAS_MAIN_TYPE= "hasMainType"; //$NON-NLS-1$

	public PropertyExpression(IConfigurationElement element) {
		fName= element.getAttribute(ATT_NAME);
		fValue= element.getAttribute(VALUE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object element) {
		if (PROPERTY_NAME.equals(fName)) { //$NON-NLS-1$
			if (!(element instanceof IResource))
				return false;
			String name= ((IResource)element).getName();
			StringMatcher matcher= new StringMatcher(fValue, false, false);
			return matcher.match(name);
		} else if (PROPERTY_INSTANCE_OF.equals(fName)) {
			return isInstanceOf(element, fValue);
		} else if (PROPERTY_HAS_MAIN_TYPE.equals(fName)) {
			if (element instanceof IType) {
				try {
					return JavaModelUtil.hasMainMethod((IType)element);
				} catch (JavaModelException e) {
					return false;
				}
			}
		}
		return false;
	}
}
