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

public final class OrOrSubTypeConstraint2 implements ITypeConstraint2 {
	
	private final ConstraintVariable2 fLeft;
	private final ConstraintVariable2 fRight;

	public OrOrSubTypeConstraint2(ConstraintVariable2 left, ConstraintVariable2 right) {
		Assert.isNotNull(left);
		Assert.isNotNull(right);
		fLeft= left;
		fRight= right;
	}

	public ConstraintVariable2 getLeft() {
		return fLeft;
	}

	public ConstraintVariable2 getRight() {
		return fRight;
	}

	public  String toString(){
		return getLeft().toString() + " <= " + getRight().toString() + " or " + getRight().toString() + " <= " + getLeft().toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		// can use object identity on ConstraintVariables, since we have the stored (or to be stored) objects
		if (other.getClass() != OrOrSubTypeConstraint2.class)
			return false;
		
		ITypeConstraint2 otherTC= (ITypeConstraint2) other;
		return getLeft() == otherTC.getLeft()
				&& getRight() == otherTC.getRight();
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getLeft().hashCode() ^ 31 * getRight().hashCode();
	}
}