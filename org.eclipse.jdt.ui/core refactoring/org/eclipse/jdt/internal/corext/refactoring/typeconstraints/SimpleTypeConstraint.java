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

import org.eclipse.jdt.internal.corext.Assert;

public final class SimpleTypeConstraint implements ITypeConstraint {
	
	private final ConstraintVariable fLeft;
	private final ConstraintVariable fRight;
	private final ConstraintOperator fOperator;
	
	private SimpleTypeConstraint(ConstraintVariable left, ConstraintVariable right, ConstraintOperator operator) {
		Assert.isNotNull(left);
		Assert.isNotNull(right);
		Assert.isNotNull(operator);
		fLeft= left;
		fRight= right;
		fOperator= operator;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public  boolean equals(Object obj) {
		if (!(obj instanceof SimpleTypeConstraint))
			return false;
		SimpleTypeConstraint other= (SimpleTypeConstraint)obj;
		return getLeft().equals(other.getLeft()) && fOperator.equals(other.fOperator) && getRight().equals(other.getRight());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public  int hashCode() {
		return getLeft().hashCode() ^ fOperator.hashCode() ^ getRight().hashCode();
	}

	public  ConstraintVariable getLeft() {
		return fLeft;
	}

	public  ConstraintVariable getRight() {
		return fRight;
	}

	public ConstraintOperator getOperator() {
		return fOperator;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public  String toString(){
		return getLeft().toString() + " " + fOperator.toString() + " " + getRight().toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.experiments.TypeConstraint#toResolvedString()
	 */
	public  String toResolvedString() {
		return getLeft().toResolvedString() + " " + fOperator.toString() + " " + getRight().toResolvedString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ITypeConstraint#isSatisfied()
	 */
	public  boolean isSatisfied() {
		return fOperator.isSatisfied(fLeft, fRight);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.experiments.ITypeConstraint#isSimpleTypeConstraint()
	 */
	public  boolean isSimpleTypeConstraint() {
		return true;
	}
	
	public boolean isSubtypeConstraint(){
		return fOperator.isSubtypeOperator();
	}

	public boolean isStrictSubtypeConstraint(){
		return fOperator.isStrictSubtypeOperator();
	}

	public boolean isEqualsConstraint(){
		return fOperator.isEqualsOperator();
	}

	public boolean isDefinesConstraint(){
		return fOperator.isDefinesOperator();
	}

	public static SimpleTypeConstraint createConstraint(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator){
		return new SimpleTypeConstraint(v1, v2, operator);
	}
	
	public static SimpleTypeConstraint createStrictSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, StrictSubtypeOperator.create());
	}
	
	public static SimpleTypeConstraint createSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, SubtypeOperator.create());
	}

	public static SimpleTypeConstraint createEqualsConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, EqualsOperator.create());
	}

	public static SimpleTypeConstraint createDefinesConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, DefinesOperator.create());
	}
}