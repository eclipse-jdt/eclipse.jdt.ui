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

/**
 * A PlainTypeVariable is a ConstraintVariable which stands for a
 * plain type (without an updatable Source location)
 */

public class PlainTypeVariable2 extends TypeConstraintVariable2 {

	protected PlainTypeVariable2(ITypeBinding typeBinding) {
		super(typeBinding);
	}

	protected int getHash() {
		return getTypeBinding().hashCode();
	}

	protected boolean isSameAs(ConstraintVariable2 other) {
		if (this == other)
			return true;
		if (other.getClass() != PlainTypeVariable2.class)
			return false;
		
		return getTypeBinding() == ((PlainTypeVariable2) other).getTypeBinding();
	}
	
	public String toString() {
		return getTypeBinding().getName();
	}

}
