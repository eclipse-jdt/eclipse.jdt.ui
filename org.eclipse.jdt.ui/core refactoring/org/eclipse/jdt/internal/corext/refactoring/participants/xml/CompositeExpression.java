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
	
	protected TestResult evaluateAnd(IScope scope) throws CoreException {
		if (fExpressions == null)
			return TestResult.TRUE;
		TestResult result= TestResult.TRUE;
		for (Iterator iter= fExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			result= result.and(expression.evaluate(scope));
			if (result != TestResult.TRUE)
				return result;
		}
		return result;
	}
	
	protected TestResult evaluateOr(IScope scope) throws CoreException {
		if (fExpressions == null)
			return TestResult.TRUE;
		TestResult result= TestResult.FALSE;
		for (Iterator iter= fExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			result= result.or(expression.evaluate(scope));
			if (result == TestResult.TRUE)
				return result;
		}
		return result;
	}	
}
