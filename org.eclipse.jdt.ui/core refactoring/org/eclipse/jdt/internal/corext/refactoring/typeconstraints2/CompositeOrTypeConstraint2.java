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



public class CompositeOrTypeConstraint2 implements ITypeConstraint2 {
	//TODO
	//
	// This constraint is only required for 
	//
	// - up- or down-casts when casting to a class type (TODO: what about primitive types?)
	//     -> up- and down-casts have left and right sides interchanged
	//
	// - constraining the receiver of an invocation of an overridden method
	//     -> left CV is always the same
	//     -> maybe split CompositeOrTypeConstraint2 into
	//
	// => only composed of subtype constraints
	// => maybe split the two usages into separate classes?
	
	public boolean isSimpleTypeConstraint() {
		return false;
	}

	public ConstraintVariable2[] getContainedConstraintVariables() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSameAs(ITypeConstraint2 other) {
		// TODO Auto-generated method stub
		if (other.getClass() != CompositeOrTypeConstraint2.class)
			return false;
		
		CompositeOrTypeConstraint2 otherTC= (CompositeOrTypeConstraint2) other;
		return false;
	}
}
