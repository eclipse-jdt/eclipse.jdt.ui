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

public class InstanceofExpression extends Expression {

	public static final String NAME= "instanceof"; //$NON-NLS-1$
	
	private String fValue;
	
	public InstanceofExpression(IConfigurationElement element) {
		fValue= element.getAttribute(ATT_VALUE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public TestResult evaluate(IVariablePool pool) {
		Object element= pool.getDefaultVariable();
		return TestResult.valueOf(isInstanceOf(element, fValue));
	}
	
	//---- Debugging ---------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "<instanceof value=\"" + fValue + "\"/>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
