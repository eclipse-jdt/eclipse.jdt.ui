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


public abstract class SimpleTypeConstraint implements ITypeConstraint {
	
	private final ConstraintVariable fLeft;
	private final ConstraintVariable fRight;
	
	public SimpleTypeConstraint(ConstraintVariable left, ConstraintVariable right) {
		Assert.isNotNull(left);
		Assert.isNotNull(right);
		fLeft= left;
		fRight= right;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof SimpleTypeConstraint))
			return false;
		SimpleTypeConstraint other= (SimpleTypeConstraint)obj;
		return getLeft().equals(other.getLeft()) && getRight().equals(other.getRight());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getLeft().hashCode() ^ getRight().hashCode();
	}

	public final ConstraintVariable getLeft() {
		return fLeft;
	}

	public final ConstraintVariable getRight() {
		return fRight;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.experiments.ITypeConstraint#isSimpleTypeConstraint()
	 */
	public final boolean isSimpleTypeConstraint() {
		return true;
	}

}
