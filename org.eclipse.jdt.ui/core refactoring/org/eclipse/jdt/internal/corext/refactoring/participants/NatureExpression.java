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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IScope;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TestResult;


public class NatureExpression extends Expression {

	private String fValue;
	
	public static final String NAME= "nature";  //$NON-NLS-1$

	public NatureExpression(IConfigurationElement element) {
		fValue= element.getAttribute(ATT_VALUE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public TestResult evaluate(IScope scope) throws CoreException {
		IProject[] projects= (IProject[])scope.getDefaultVariable();
		for (int i= 0; i < projects.length; i++) {
			if (projects[i].hasNature(fValue))
				return TestResult.TRUE;
		}
		return TestResult.FALSE;
	}
}
