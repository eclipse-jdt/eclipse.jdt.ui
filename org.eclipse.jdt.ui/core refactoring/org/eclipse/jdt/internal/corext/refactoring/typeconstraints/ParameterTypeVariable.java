/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.dom.IMethodBinding;

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

	@Override
	public String toString() {
		return "[Parameter(" + fParameterIndex + "," + Bindings.asString(fMethodBinding) + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public IMethodBinding getMethodBinding() {
		return fMethodBinding;
	}

	public int getParameterIndex() {
		return fParameterIndex;
	}

}
