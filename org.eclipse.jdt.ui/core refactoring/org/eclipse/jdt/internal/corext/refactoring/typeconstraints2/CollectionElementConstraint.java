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


public class CollectionElementConstraint implements ITypeConstraint2 {

	private final TypeVariable2 fTypeVariable;
	private final CollectionElementVariable2 fElementVariable;

	public CollectionElementConstraint(TypeVariable2 typeVariable, CollectionElementVariable2 elementVariable) {
		fTypeVariable= typeVariable;
		fElementVariable= elementVariable;
	}

	public boolean isSimpleTypeConstraint() {
		return false;
	}

	public CollectionElementVariable2 getElementVariable() {
		return fElementVariable;
	}
	
	public TypeVariable2 getTypeVariable() {
		return fTypeVariable;
	}
	
//	public ConstraintVariable2[] getContainedConstraintVariables() {
//		return null;
//	}

	public boolean isSameAs(ITypeConstraint2 other) {
		// can use object identity on ConstraintVariables, since we have the stored (or to be stored) objects
		if (other.getClass() != CollectionElementConstraint.class)
			return false;
		
		CollectionElementConstraint otherEC= (CollectionElementConstraint) other;
		return getTypeVariable() == otherEC.getTypeVariable()
				&& getElementVariable() == otherEC.getElementVariable();
	}

	public int getHash() {
		//take the cheap hashCode() from Object rather than getHash() from ConstraintVariables
		return getTypeVariable().hashCode() ^ 37 * getElementVariable().hashCode();
	}
	
	public  String toString(){
		return getTypeVariable().toString() + ": " + getElementVariable().toString(); //$NON-NLS-1$
	}

}
