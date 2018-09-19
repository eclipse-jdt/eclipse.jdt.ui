/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

public final class TypeVariable extends ConstraintVariable {

	private final String fSource;
	private final CompilationUnitRange fTypeRange;

	public TypeVariable(Type type){
		super(type.resolveBinding());
		fSource= type.toString();
		ICompilationUnit cu= ASTCreator.getCu(type);
		Assert.isNotNull(cu);
		fTypeRange= new CompilationUnitRange(cu, ASTNodes.getElementType(type));
	}

	public TypeVariable(ITypeBinding binding, String source, CompilationUnitRange range){
		super(binding);
		fSource= source;
		fTypeRange= range;
	}

	@Override
	public String toString() {
		return fSource;
	}

	public CompilationUnitRange getCompilationUnitRange() {
		return fTypeRange;
	}
}
