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

import org.eclipse.jdt.core.ICompilationUnit;

public class CollectionElementVariable2 extends TypeConstraintVariable2 {

	private final ConstraintVariable2 fElementCv;
	private EquivalenceRepresentative fRepresentative;

	//TODO: make a 'TypedCollectionElementVariable extends TypeConstraintVariable2'
	// iff Collection reference already has type parameter in source
	public CollectionElementVariable2(ConstraintVariable2 elementCv) {
		fElementCv= elementCv;
	}

	protected int getHash() {
		return fElementCv.hashCode();
	}

	protected boolean isSameAs(ConstraintVariable2 other) {
		if (this == other)
			return true;
		if (other.getClass() != CollectionElementVariable2.class)
			return false;
		
		return fElementCv == ((CollectionElementVariable2) other).fElementCv;
	}

	public ConstraintVariable2 getElementVariable() {
		return fElementCv;
	}
	
	public ICompilationUnit getCompilationUnit() {
		if (fElementCv instanceof IDeclaredConstraintVariable)
			return ((IDeclaredConstraintVariable) fElementCv).getCompilationUnit();
		else { //TODO: assert in constructor(s)
			return ((CollectionElementVariable2) fElementCv).getCompilationUnit();
		}
	}
	
	public EquivalenceRepresentative getRepresentative() {
		return fRepresentative;
	}
	
	public void setRepresentative(EquivalenceRepresentative representatice) {
		fRepresentative= representatice;
	}
	
	public String toString() {
		return "Elem[" + fElementCv.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
