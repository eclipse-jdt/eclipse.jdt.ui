/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.dom.Bindings;

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
	
	/* (non-Javadoc)
	* @see java.lang.Object#equals(java.lang.Object)
	*/
	public boolean equals(Object obj) {
		if (!(obj instanceof ConstraintVariable))
			return false;
		ConstraintVariable other= (ConstraintVariable) obj;
		return TypeBindings.isEqualTo(fBinding, other.fBinding);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (fBinding == null)
			return super.hashCode();
		return Bindings.hashCode(fBinding);
	}
	
	public ITypeBinding getBinding() {
		return fBinding;
	}

}
