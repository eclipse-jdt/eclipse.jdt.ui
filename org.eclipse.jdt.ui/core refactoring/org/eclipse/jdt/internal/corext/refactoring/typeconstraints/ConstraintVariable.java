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

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.TypeRules;

public abstract class ConstraintVariable {
	/**
	 * The type binding, or <code>null</code>.
	 */
	private final ITypeBinding fBinding;

	/**
	 * @param binding the type binding, or <code>null</code>
	 */
	protected ConstraintVariable(ITypeBinding binding) {
		fBinding= binding;
	}

	public boolean canBeAssignedTo(ConstraintVariable targetVariable) {
		if (fBinding == null || targetVariable.fBinding == null)
			return false;
		return TypeRules.canAssign(fBinding, targetVariable.fBinding);
	}

	public String toResolvedString() {
		if (fBinding == null)
			return "<NULL BINDING>"; //$NON-NLS-1$
		return Bindings.asString(fBinding);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toResolvedString();
	}
		
	/**
	 * @return the type binding or <code>null</code>
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
