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

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;


public interface ITypeConstraintFactory {
	
	public ITypeConstraint[] createSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2);
	public ITypeConstraint[] createEqualsConstraint(ConstraintVariable v1, ConstraintVariable v2);
	public ITypeConstraint[] createDefinesConstraint(ConstraintVariable v1, ConstraintVariable v2);
	public ITypeConstraint[] createStrictSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2);
	
	public CompositeOrTypeConstraint createCompositeOrTypeConstraint(ITypeConstraint[] constraints);
	
	/**
	 * Allows for avoiding the creation of SimpleTypeConstraints based on properties of
	 * their constituent ConstraintVariables and ConstraintOperators. Can be used to e.g. 
	 * avoid creation of constraints for assignments between built-in types.
	 */
	public boolean filter(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator);
	
}
