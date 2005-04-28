/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	protected void initialize(ITypeBinding binding) {
		super.initialize(binding);
		ITypeBinding[] bounds= binding.getTypeBounds();
		fBounds= new TType[bounds.length];
		for (int i= 0; i < bounds.length; i++) {
			fBounds[i]= getEnvironment().create(bounds[i]);
		}
	}
	
	public TType getErasure() {
		return fBounds[0].getErasure();
	}
	
	/* package */ final TType getLeftMostBound() {
		return fBounds[0];
	}
	
	public final TType[] getBounds() {
		return (TType[]) fBounds.clone();
	}
	
	public final TType[] getSubTypes() {
		throw new UnsupportedOperationException();
	}
	
	protected final boolean checkAssignmentBound(TType rhs) {
		if (fBounds.length == 0)
			return true;
		for (int i= 0; i < fBounds.length; i++) {
			if (rhs.canAssignTo(fBounds[i]))
				return true;
		}
		return false;
	}
	
	protected final boolean canAssignOneBoundTo(TType lhs) {
		if (fBounds.length == 0)
			return true;
		for (int i= 0; i < fBounds.length; i++) {
			if (fBounds[i].canAssignTo(lhs))
				return true;
		}
		return false;
	}
	
}
