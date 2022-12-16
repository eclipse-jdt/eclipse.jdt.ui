/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure.constraints;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;

/**
 * Type constraint which models covariance-related types.
 */
public final class CovariantTypeConstraint implements ITypeConstraint2 {

	/** The ancestor type */
	private final ConstraintVariable2 fAncestor;

	/** The descendant type */
	private final ConstraintVariable2 fDescendant;

	/**
	 * Creates a new covariant type constraint.
	 *
	 * @param descendant the descendant type
	 * @param ancestor the ancestor type
	 */
	public CovariantTypeConstraint(final ConstraintVariable2 descendant, final ConstraintVariable2 ancestor) {
		Assert.isNotNull(descendant);
		Assert.isNotNull(ancestor);
		fDescendant= descendant;
		fAncestor= ancestor;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object object) {
		if (object.getClass() != CovariantTypeConstraint.class)
			return false;
		final ITypeConstraint2 other= (ITypeConstraint2) object;
		return getLeft() == other.getLeft() && getRight() == other.getRight();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2#getLeft()
	 */
	@Override
	public ConstraintVariable2 getLeft() {
		return fDescendant;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2#getRight()
	 */
	@Override
	public ConstraintVariable2 getRight() {
		return fAncestor;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return fDescendant.hashCode() ^ 35 * fAncestor.hashCode();
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return fDescendant.toString() + " <<= " + fAncestor.toString(); //$NON-NLS-1$
	}
}
