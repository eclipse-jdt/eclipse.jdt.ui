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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.ITypeBinding;

public abstract class ConstraintVariable {
	private final ITypeBinding fBinding;

	protected ConstraintVariable(ITypeBinding binding) {
		fBinding= binding;//can be null
	}

	public boolean isEqualType(ConstraintVariable other) {
		return isEqualBinding(other.fBinding);
	}

	public boolean isStrictSubtypeOf(ConstraintVariable other) {
		return TypeBindings.isSubtypeBindingOf(fBinding, other.fBinding);
	}

	public boolean isSubtypeOf(ConstraintVariable other) {
		return isEqualType(other) || isStrictSubtypeOf(other);
	}

	public boolean isEqualBinding(ITypeBinding binding){
		return TypeBindings.isEqualTo(fBinding, binding);
	}
	
	public String toResolvedString() {
		return TypeBindings.toString(fBinding);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return TypeBindings.toString(fBinding);
	}
		
	/**
	 * can be <code>null</code>.
	 */
	public ITypeBinding getBinding() {
		return fBinding;
	}

	/**
	 * For storing additional information associated with constraint variables. 
	 * Added in anticipation of the generics-related refactorings.
	 */
	private Object fData;
	
	public Object getData(){
		return fData;
	}
	
	public void setData(Object data){
		fData= data;
	}
}
