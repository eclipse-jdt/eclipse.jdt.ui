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

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;


public class CastVariable2 extends TypeConstraintVariable2 {

	private final CompilationUnitRange fRange;
	private TypeConstraintVariable2 fExpressionVariable;

	protected CastVariable2(TType type, CompilationUnitRange range, TypeConstraintVariable2 expressionVariable) {
		super(type);
		fRange= range;
		fExpressionVariable= expressionVariable;
	}
	
	public CompilationUnitRange getRange() {
		return fRange;
	}
	
	public ICompilationUnit getCompilationUnit() {
		return fRange.getCompilationUnit();
	}
	
	public TypeConstraintVariable2 getExpressionVariable() {
		return fExpressionVariable;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2#getHash()
	 */
	protected int getHash() {
		return getRange().hashCode() ^ getType().hashCode();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2#isSameAs(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2)
	 */
	protected boolean isSameAs(ConstraintVariable2 other) {
		return this == other; // unique per construction
	}

}
