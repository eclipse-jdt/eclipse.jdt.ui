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

import org.eclipse.jdt.core.dom.IMethodBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;

public class ParameterTypeVariable extends ConstraintVariable {

	private final IMethodBinding fMethodBinding;
	private final int fParameterIndex;
	
	public ParameterTypeVariable(IMethodBinding methodBinding, int parameterIndex) {
		super(methodBinding.getParameterTypes()[parameterIndex]);
		Assert.isNotNull(methodBinding);
		Assert.isTrue(0 <= parameterIndex);
		Assert.isTrue(parameterIndex < methodBinding.getParameterTypes().length);
		fMethodBinding= methodBinding;
		fParameterIndex= parameterIndex;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[Parameter(" + fParameterIndex + "," + Bindings.asString(fMethodBinding) + ")]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (! super.equals(obj))
			return false;
		if (! (obj instanceof ParameterTypeVariable))
			return false;
		ParameterTypeVariable other= (ParameterTypeVariable)obj;
		return Bindings.equals(fMethodBinding, other.fMethodBinding) && fParameterIndex == other.fParameterIndex;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() ^ fMethodBinding.hashCode() ^ fParameterIndex;
	}

}
