/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.Expression;

public class ExpressionVariable extends ConstraintVariable {
	
	private final Expression fExpression;
	
	public ExpressionVariable(Expression expression){
		super(expression.resolveTypeBinding());
		fExpression= expression;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[" + fExpression.toString() + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (! super.equals(obj))
			return false;
		if (! (obj instanceof ExpressionVariable))
			return false;
		ExpressionVariable other= (ExpressionVariable)obj;
		return Expressions.equalBindings(fExpression, other.fExpression);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() ^ Expressions.hashCode(fExpression);
	}
}