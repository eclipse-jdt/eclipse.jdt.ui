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

import org.eclipse.jdt.internal.corext.Assert;


/**
 * A {@link TypeConstraintVariable2} stands for an AST entity which has a type.
 */

public abstract class TypeConstraintVariable2 extends ConstraintVariable2 {

	private TypeHandle fTypeHandle;

	/**
	 * @param typeHandle the type handle
	 */
	protected TypeConstraintVariable2(TypeHandle typeHandle) {
		Assert.isNotNull(typeHandle);
		fTypeHandle= typeHandle;
//		if (DEBUG)
//			setData(TO_STRING, Bindings.asString(typeBinding));
	}
	
	/**
	 * Create a new type constraint variable without an
	 * associated type handle.
	 */
	protected TypeConstraintVariable2() {
		// nothing to do
	}

	/**
	 * @return the type handle, or <code>null</code> iff the type constraint
	 *         variable has no type in the original source (e.g.
	 *         {@link CollectionElementVariable2})
	 */
	public TypeHandle getTypeHandle() {
		return fTypeHandle;
	}
	
	
	public String toString() {
		String toString= (String) getData(TO_STRING);
		if (toString != null)
			return toString;
			
		String name= getClass().getName();
		int dot= name.lastIndexOf('.');
		return name.substring(dot + 1) + ": " + fTypeHandle.getSimpleName(); //$NON-NLS-1$
	}

}
