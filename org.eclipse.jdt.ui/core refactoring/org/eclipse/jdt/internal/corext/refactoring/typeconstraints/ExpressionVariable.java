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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;

public final class ExpressionVariable extends ConstraintVariable {
	
	private final CompilationUnitRange fRange;
	private final String fSource;
	private final IBinding fExpressionBinding;
	private final int fExpressionType;
		
	public ExpressionVariable(Expression expression){
		super(expression.resolveTypeBinding());
		fSource= expression.toString();
		ICompilationUnit cu= ASTCreator.getCu(expression);
		Assert.isNotNull(cu);
		fRange= new CompilationUnitRange(cu, expression);
		fExpressionBinding= resolveBinding(expression);
		fExpressionType= expression.getNodeType();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[" + fSource + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (! super.equals(obj))
			return false;
		if (! (obj instanceof ExpressionVariable))
			return false;
		ExpressionVariable other= (ExpressionVariable)obj;

		if (fRange.equals(other.fRange))
			return true;
			
		if (fExpressionBinding instanceof ITypeBinding){
			//if the expression resolves to a type binding, then the expressions should be the same
			//we checked already that they are not, so false
			return false;
		}
		if (fExpressionBinding == null)
			return false;
		return Bindings.equals(fExpressionBinding, other.fExpressionBinding);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (fExpressionBinding != null)
			return super.hashCode() ^ Bindings.hashCode(fExpressionBinding);
		else
			return super.hashCode() ^ fRange.hashCode();
	}
	
	public CompilationUnitRange getCompilationUnitRange() {
		return fRange;
	}
	
	public int getExpressionType() {
		return fExpressionType;
	}

	public IBinding getExpressionBinding() {
		return fExpressionBinding;
	}
	
	private static IBinding resolveBinding(Expression expression){
		if (expression instanceof Name)
			return ((Name)expression).resolveBinding();
		if (expression instanceof ParenthesizedExpression)
			return resolveBinding(((ParenthesizedExpression)expression).getExpression());
		else if (expression instanceof Assignment)
			return resolveBinding(((Assignment)expression).getLeftHandSide());//TODO ???
		else if (expression instanceof MethodInvocation)
			return ((MethodInvocation)expression).resolveMethodBinding();
		else if (expression instanceof SuperMethodInvocation)
			return ((SuperMethodInvocation)expression).resolveMethodBinding();
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).resolveFieldBinding();
		else if (expression instanceof SuperFieldAccess)
			return ((SuperFieldAccess)expression).resolveFieldBinding();
		else if (expression instanceof ConditionalExpression)
			return resolveBinding(((ConditionalExpression)expression).getThenExpression());
		return null;
	}
}