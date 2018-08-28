/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.ArrayType;


public final class ArrayElementVariable2 extends ConstraintVariable2 {

	private final ConstraintVariable2 fParentCv;

	public ArrayElementVariable2(ConstraintVariable2 parentCv) {
		super(((ArrayType) parentCv.getType()).getComponentType());
		fParentCv= parentCv;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return fParentCv.hashCode();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other.getClass() != ArrayElementVariable2.class)
			return false;

		ArrayElementVariable2 other2= (ArrayElementVariable2) other;
		return fParentCv == other2.fParentCv;
	}

}
