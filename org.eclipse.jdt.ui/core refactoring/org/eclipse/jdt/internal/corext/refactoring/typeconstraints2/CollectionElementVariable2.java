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
import org.eclipse.jdt.core.dom.ITypeBinding;

public class CollectionElementVariable2 extends TypeConstraintVariable2 {

	public static final int NOT_DECLARED_TYPE_VARIABLE_INDEX= -1;
	
	private final ConstraintVariable2 fParentCv;
	private final String fTypeVariableKey;
	private final int fDeclarationTypeVariableIndex;

	//TODO: make a 'TypedCollectionElementVariable extends TypeConstraintVariable2'
	// iff Collection reference already has type parameter in source
	/**
	 * @param parentCv the parent constraint variable
	 * @param typeVariable the type variable for this constraint
	 * @param declarationTypeVariableIndex
	 */
	public CollectionElementVariable2(ConstraintVariable2 parentCv, ITypeBinding typeVariable, int declarationTypeVariableIndex) {
		fParentCv= parentCv;
		if (! typeVariable.isTypeVariable())
			throw new IllegalArgumentException(typeVariable.toString());
		fTypeVariableKey= typeVariable.getKey();
		fDeclarationTypeVariableIndex= declarationTypeVariableIndex;
	}

	protected int getHash() {
		return fParentCv.hashCode() ^ fTypeVariableKey.hashCode();
	}

	protected boolean isSameAs(ConstraintVariable2 other) {
		if (this == other)
			return true;
		if (other.getClass() != CollectionElementVariable2.class)
			return false;
		
		CollectionElementVariable2 other2= (CollectionElementVariable2) other;
		return fParentCv == other2.fParentCv
				&& fTypeVariableKey.equals(other2.fTypeVariableKey);
	}
	
	public int getDeclarationTypeVariableIndex() {
		return fDeclarationTypeVariableIndex;
	}
	
	public ConstraintVariable2 getParentConstraintVariable() {
		return fParentCv;
	}
	
	public ICompilationUnit getCompilationUnit() {
		if (fParentCv instanceof IDeclaredConstraintVariable)
			return ((IDeclaredConstraintVariable) fParentCv).getCompilationUnit();
		else
			return null;
//			//TODO: assert in constructor(s)
//			return ((CollectionElementVariable2) fElementCv).getCompilationUnit();
	}
	
	public String toString() {
		return "Elem[" + fParentCv.toString() + ", " + fTypeVariableKey + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
