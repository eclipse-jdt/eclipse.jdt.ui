/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.jdt.internal.corext.Assert;

public final class TypeConstraint2 {
	
	private final ConstraintVariable2 fLeft;
	private final ConstraintVariable2 fRight;
	private final ConstraintOperator2 fOperator;
	
	public TypeConstraint2(ConstraintVariable2 left, ConstraintVariable2 right, ConstraintOperator2 operator) {
		Assert.isNotNull(left);
		Assert.isNotNull(right);
		Assert.isNotNull(operator);
		fLeft= left;
		fRight= right;
		fOperator= operator;
	}

	public ConstraintVariable2 getLeft() {
		return fLeft;
	}

	public ConstraintVariable2 getRight() {
		return fRight;
	}

	public ConstraintOperator2 getOperator() {
		return fOperator;
	}

	public  String toString(){
		return getLeft().toString() + " " + fOperator.toString() + " " + getRight().toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		// can use object identity on ConstraintVariables, since we have the stored (or to be stored) objects
		if (other.getClass() != TypeConstraint2.class)
			return false;
		
		TypeConstraint2 otherTC= (TypeConstraint2) other;
		return getLeft() == otherTC.getLeft()
				&& getRight() == otherTC.getRight()
				&& getOperator() == otherTC.getOperator();
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getLeft().hashCode() ^ 37 * getRight().hashCode() ^ 37 * getOperator().hashCode();
	}
}
