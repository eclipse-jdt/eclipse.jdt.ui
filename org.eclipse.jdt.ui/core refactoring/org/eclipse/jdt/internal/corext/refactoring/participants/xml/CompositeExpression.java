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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

public abstract class CompositeExpression extends Expression {
	
	private static final Expression[] EMPTY_ARRAY= new Expression[0]; 
	
	protected List fExpressions;
	
	public CompositeExpression() {
	}

	public void add(Expression expression) {
		if (fExpressions == null)
			fExpressions= new ArrayList(2);
		fExpressions.add(expression);
	}
	
	public Expression[] getChildren() {
		if (fExpressions == null)
			return EMPTY_ARRAY;
		return (Expression[])fExpressions.toArray(new Expression[fExpressions.size()]);
	}
	
	protected int evaluateAnd(Object element) throws CoreException {
		if (fExpressions == null)
			return ITestResult.TRUE;
		int result= ITestResult.TRUE;
		for (Iterator iter= fExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			result= TestResult.and(result, expression.evaluate(element));
			if (result != ITestResult.TRUE)
				return result;
		}
		return result;
	}
	
	protected int evaluateOr(Object element) throws CoreException {
		if (fExpressions == null)
			return ITestResult.TRUE;
		int result= ITestResult.FALSE;
		for (Iterator iter= fExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			result= TestResult.or(result, expression.evaluate(element));
			if (result == ITestResult.TRUE)
				return result;
		}
		return result;
	}	
}
