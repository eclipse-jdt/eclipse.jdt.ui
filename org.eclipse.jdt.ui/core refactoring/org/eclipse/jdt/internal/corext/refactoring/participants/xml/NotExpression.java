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

import org.eclipse.jdt.internal.corext.Assert;


public class NotExpression extends Expression {

	private Expression fExpression;

	public static final String NAME= "not"; //$NON-NLS-1$

	private static final int[] NOT= new int[] {
		//FALSE				//TRUE				//NOT_LOADED			//UNKNOWN
		ITestResult.TRUE,	ITestResult.FALSE,	ITestResult.NOT_LOADED,	ITestResult.UNKNOWN
	};

	private static int not(int op) {
		return NOT[op];
	}
	
	
	public NotExpression(Expression expression) {
		Assert.isNotNull(expression);
		fExpression= expression;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public int evaluate(Object element) throws CoreException {
		return not(fExpression.evaluate(element));
	}
}
