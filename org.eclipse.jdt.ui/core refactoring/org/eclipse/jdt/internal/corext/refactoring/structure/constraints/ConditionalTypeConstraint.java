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
 * Type constraint which models conditional expression type constraints.
 */
public final class ConditionalTypeConstraint implements ITypeConstraint2 {

	/** The else type variable */
	private final ConstraintVariable2 fElseVariable;

	/** The expression type variable */
	private final ConstraintVariable2 fExpressionVariable;

	/** The then type variable */
	private final ConstraintVariable2 fThenVariable;

	/**
	 * Creates a new conditional type constraint.
	 *
	 * @param expressionVariable the expression type constraint variable
	 * @param thenVariable the then type constraint variable
	 * @param elseVariable the else type constraint variable
	 */
	public ConditionalTypeConstraint(final ConstraintVariable2 expressionVariable, final ConstraintVariable2 thenVariable, final ConstraintVariable2 elseVariable) {
		Assert.isNotNull(expressionVariable);
		Assert.isNotNull(thenVariable);
		Assert.isNotNull(elseVariable);
		fExpressionVariable= expressionVariable;
		fThenVariable= thenVariable;
		fElseVariable= elseVariable;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object object) {
		if (object.getClass() != ConditionalTypeConstraint.class)
			return false;
		final ITypeConstraint2 other= (ITypeConstraint2) object;
		return getLeft() == other.getLeft() && getRight() == other.getRight();
	}

	/**
	 * Returns the expression type constraint variable.
	 *
	 * @return the expression type constraint variable
	 */
	public ConstraintVariable2 getExpression() {
		return fExpressionVariable;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2#getLeft()
	 */
	@Override
	public ConstraintVariable2 getLeft() {
		return fThenVariable;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2#getRight()
	 */
	@Override
	public ConstraintVariable2 getRight() {
		return fElseVariable;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return fThenVariable.hashCode() ^ 33 * fElseVariable.hashCode();
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return fThenVariable.toString() + " <?= " + fElseVariable.toString(); //$NON-NLS-1$
	}
}
