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
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintOperator2;

public final class SimpleTypeConstraint2 implements ITypeConstraint2 {
	
	private final ConstraintVariable2 fLeft;
	private final ConstraintVariable2 fRight;
	private final ConstraintOperator2 fOperator;
	
	/* package */ SimpleTypeConstraint2(ConstraintVariable2 left, ConstraintVariable2 right, ConstraintOperator2 operator) {
		Assert.isNotNull(left);
		Assert.isNotNull(right);
		Assert.isNotNull(operator);
		fLeft= left;
		fRight= right;
		fOperator= operator;
	}
	
	public  ConstraintVariable2 getLeft() {
		return fLeft;
	}

	public  ConstraintVariable2 getRight() {
		return fRight;
	}

	public ConstraintOperator2 getOperator() {
		return fOperator;
	}
	
	public ConstraintVariable2[] getContainedConstraintVariables() {
		return new ConstraintVariable2[] {getLeft(), getRight()};
	}
	
	public  String toString(){
		return getLeft().toString() + " " + fOperator.toString() + " " + getRight().toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isSimpleTypeConstraint() {
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
	
	
	public boolean isSameAs(ITypeConstraint2 other) {
		if (other.getClass() != SimpleTypeConstraint2.class)
			return false;
		
		SimpleTypeConstraint2 otherTC= (SimpleTypeConstraint2) other;
		return getLeft() == otherTC.getLeft()
				&& getRight() == otherTC.getRight()
				&& getOperator() == otherTC.getOperator();
	}
}
