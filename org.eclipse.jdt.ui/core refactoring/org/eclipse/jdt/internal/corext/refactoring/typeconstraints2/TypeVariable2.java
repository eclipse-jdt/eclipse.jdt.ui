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

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;

/**
 * A TypeVariable is a ConstraintVariable which stands for a
 * type reference (in source).
 */
public class TypeVariable2 extends ConstraintVariable2 {

	private final CompilationUnitRange fRange;

	protected TypeVariable2(TypeHandle typeHandle, CompilationUnitRange range) {
		super(typeHandle);
		fRange= range;
	}
	
	public CompilationUnitRange getRange() {
		return fRange;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2#getHash()
	 */
	protected int getHash() {
		return getRange().hashCode() ^ getTypeHandle().hashCode();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2#isSameAs(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2)
	 */
	protected boolean isSameAs(ConstraintVariable2 other) {
		if (this == other)
			return true;
		if (other.getClass() != TypeVariable2.class)
			return false;
		
		return getRange().equals(((TypeVariable2) other).getRange())
				&& getTypeHandle() == other.getTypeHandle();
	}
	
}
