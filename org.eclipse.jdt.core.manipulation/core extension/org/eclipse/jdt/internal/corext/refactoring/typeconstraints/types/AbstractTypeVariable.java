/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.jdt.core.dom.ITypeBinding;


public abstract class AbstractTypeVariable extends TType {

	protected TType[] fBounds;

	protected AbstractTypeVariable(TypeEnvironment environment) {
		super(environment);
	}

	@Override
	protected void initialize(ITypeBinding binding) {
		super.initialize(binding);
		ITypeBinding[] bounds= binding.getTypeBounds();
		if (bounds.length == 0) {
			fBounds= EMPTY_TYPE_ARRAY;
			if (getEnvironment().getJavaLangObject() == null) {
				getEnvironment().initializeJavaLangObject(binding.getErasure());
			}
		} else {
			fBounds= new TType[bounds.length];
			for (int i= 0; i < bounds.length; i++) {
				fBounds[i]= getEnvironment().create(bounds[i]);
			}
		}
	}

	@Override
	public TType getErasure() {
		if (fBounds.length == 0)
			return getEnvironment().getJavaLangObject();
		return fBounds[0].getErasure();
	}

	/* package */ final boolean isUnbounded() {
		if (fBounds.length == 0)
			return true;
		return fBounds[0].isJavaLangObject();
	}

	public final TType[] getBounds() {
		return fBounds.clone();
	}

	@Override
	public final TType[] getSubTypes() {
		return EMPTY_TYPE_ARRAY;
	}

	protected final boolean checkAssignmentBound(TType rhs) {
		if (fBounds.length == 0)
			return true;
		for (TType bound : fBounds) {
			if (rhs.canAssignTo(bound)) {
				return true;
			}
		}
		return false;
	}

	protected final boolean canAssignOneBoundTo(TType lhs) {
		if (fBounds.length == 0)
			return lhs.isJavaLangObject();
		for (TType bound : fBounds) {
			if (bound.canAssignTo(lhs)) {
				return true;
			}
		}
		return false;
	}

}
