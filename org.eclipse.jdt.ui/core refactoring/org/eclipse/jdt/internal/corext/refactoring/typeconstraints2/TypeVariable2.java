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

import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;

public class TypeVariable2 extends ConstraintVariable2 {

	private final CompilationUnitRange fRange;

	protected TypeVariable2(TypeHandle typeHandle, ITypeBinding typeBinding, CompilationUnitRange range) {
		super(typeHandle, typeBinding);
		fRange= range;
	}
	
	public CompilationUnitRange getRange() {
		return fRange;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2#getHash()
	 */
	public int getHash() {
		return getRange().hashCode();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2#isSameAs(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2)
	 */
	public boolean isSameAs(ConstraintVariable2 other) {
		if (this == other)
			return true;
		if (other.getClass() != TypeVariable2.class)
			return false;
		
		return getRange().equals(((TypeVariable2) other).getRange());
	}
	
}
