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

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A {@link TypeConstraintVariable2} stands for an AST entity which has a type.
 */

public abstract class TypeConstraintVariable2 extends ConstraintVariable2 {

	private ITypeBinding fTypeBinding;

	/**
	 * @param typeBindings the type binding
	 */
	protected TypeConstraintVariable2(ITypeBinding typeBindings) {
		Assert.isNotNull(typeBindings);
		fTypeBinding= typeBindings;
	}
	
	/**
	 * Create a new type constraint variable without an
	 * associated type binding.
	 */
	protected TypeConstraintVariable2() {
		// nothing to do
	}

	/**
	 * @return the type binding, or <code>null</code> iff the type constraint
	 *         variable has no type in the original source (e.g.
	 *         {@link CollectionElementVariable2})
	 */
	public ITypeBinding getTypeBinding() {
		return fTypeBinding;
	}
	
	public String toString() {
		String toString= (String) getData(TO_STRING);
		if (toString != null)
			return toString;
			
		String name= getClass().getName();
		int dot= name.lastIndexOf('.');
		return name.substring(dot + 1) + ": " + fTypeBinding.getName(); //$NON-NLS-1$
	}

}
