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

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;

/**
 * A PlainTypeVariable is a ConstraintVariable which stands for a
 * plain type (without an updatable Source location)
 */

public class PlainTypeVariable2 extends TypeConstraintVariable2 {

	protected PlainTypeVariable2(TType type) {
		super(type);
	}

	protected int getHash() {
		return getType().hashCode();
	}

	protected boolean isSameAs(ConstraintVariable2 other) {
		if (this == other)
			return true;
		if (other.getClass() != PlainTypeVariable2.class)
			return false;
		
		return getType() == ((PlainTypeVariable2) other).getType();
	}
	
	public String toString() {
		return getType().getPrettySignature();
	}

}
