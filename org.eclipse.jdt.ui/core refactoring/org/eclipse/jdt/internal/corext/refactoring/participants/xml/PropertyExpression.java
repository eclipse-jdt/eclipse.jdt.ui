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

import org.eclipse.core.runtime.IConfigurationElement;



public class PropertyExpression extends Expression {

	private String fName;
	private String fValue;
	
	public static final String NAME= "property"; //$NON-NLS-1$
	private static final String ATT_NAME= "name"; //$NON-NLS-1$
	
	private static final String PROPERTY_INSTANCE_OF= "instanceof";	 //$NON-NLS-1$

	public PropertyExpression(IConfigurationElement element) {
		fName= element.getAttribute(ATT_NAME);
		fValue= element.getAttribute(VALUE);
	}
	
	public String getName() {
		return fName;
	}
	
	public String getValue() {
		return fValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public int evaluate(Object element) {
		if (PROPERTY_INSTANCE_OF.equals(fName)) {
			return isInstanceOf(element, fValue) ? ITestResult.TRUE : ITestResult.FALSE;
		} else {
			PropertyInterface pi= PropertyInterface.get(element.getClass());
			return pi.test(element, fName, fValue);
		}
	}
	
	//---- Debugging ---------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "<property name=\"" + fName + "\" value=\"" + fValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
