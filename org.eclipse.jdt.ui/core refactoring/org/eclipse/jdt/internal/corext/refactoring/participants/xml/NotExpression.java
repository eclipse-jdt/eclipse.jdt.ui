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

import org.eclipse.jdt.internal.corext.Assert;


public class NotExpression extends Expression {

	private Expression fExpression;

	public static final String NAME= "not"; //$NON-NLS-1$

	
	public NotExpression(Expression expression) {
		Assert.isNotNull(expression);
		fExpression= expression;
	}
	
	public TestResult evaluate(IVariablePool pool) throws ExpressionException {
		return fExpression.evaluate(pool).not();
	}
}
