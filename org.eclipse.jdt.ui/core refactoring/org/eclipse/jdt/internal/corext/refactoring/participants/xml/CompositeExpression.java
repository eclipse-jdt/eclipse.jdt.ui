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
	
	protected List fExpressions;
	
	public CompositeExpression() {
	}

	public void add(Expression expression) {
		if (fExpressions == null)
			fExpressions= new ArrayList(2);
		fExpressions.add(expression);
	}
	
	public String[] getPropertyValues(String name) {
		List result= new ArrayList(2);
		getPropertyExpressions(result, this, name);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	private static void getPropertyExpressions(List result, CompositeExpression root, String name) {
		if (root.fExpressions == null)
			return;
		for (Iterator iter= root.fExpressions.iterator(); iter.hasNext();) {
			Expression element= (Expression)iter.next();
			if (element instanceof PropertyExpression) {
				PropertyExpression pe= (PropertyExpression)element;
				if (name.equals(pe.getName()))
					result.add(pe.getValue());
			} else if (element instanceof CompositeExpression) {
				getPropertyExpressions(result, (CompositeExpression)element, name);
			}
		}
		
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
