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

package org.eclipse.jdt.internal.corext.refactoring.generics;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;


public abstract class SpecialMethod {

	protected final ITypeBinding fTypeBinding;
	protected final String fName;
	protected final ITypeBinding[] fParameterTypes;

	public SpecialMethod(ITypeBinding typeBinding, String name, ITypeBinding[] argumentTypes) {
		fTypeBinding= typeBinding;
		fName= name;
		fParameterTypes= argumentTypes;
	}

	public abstract void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator creator);

}
