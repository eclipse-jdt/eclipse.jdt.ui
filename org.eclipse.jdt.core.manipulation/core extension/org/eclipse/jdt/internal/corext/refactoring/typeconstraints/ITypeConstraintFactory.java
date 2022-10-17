/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;


public interface ITypeConstraintFactory {

	ITypeConstraint[] createSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2);
	ITypeConstraint[] createEqualsConstraint(ConstraintVariable v1, ConstraintVariable v2);
	ITypeConstraint[] createDefinesConstraint(ConstraintVariable v1, ConstraintVariable v2);
	ITypeConstraint[] createStrictSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2);

	CompositeOrTypeConstraint createCompositeOrTypeConstraint(ITypeConstraint[] constraints);

	/**
	 * Allows for avoiding the creation of SimpleTypeConstraints based on properties of
	 * their constituent ConstraintVariables and ConstraintOperators. Can be used to e.g.
	 * avoid creation of constraints for assignments between built-in types.
	 *
	 * @param v1
	 * @param v2
	 * @param operator
	 * @return whether the constraint should <em>not</em> be created
	 */
	boolean filter(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator);

}
