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
	
	protected boolean evaluateAnd(Object element) throws CoreException {
		if (fExpressions == null)
			return true;
		for (Iterator iter= fExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			if (!expression.evaluate(element))
				return false;
		}
		return true;
	}
	
	protected boolean evaluateOr(Object element) throws CoreException {
		if (fExpressions == null)
			return true;
		for (Iterator iter= fExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			if (expression.evaluate(element))
				return true;
		}
		return false;
	}	
}
