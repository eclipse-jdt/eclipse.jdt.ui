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

import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeHandle;


public abstract class SpecialMethod {

	protected final TypeHandle fTypeHandle;
	protected final String fName;
	protected final TypeHandle[] fParameterTypes;

	public SpecialMethod(TypeHandle typeHandle, String name, TypeHandle[] argumentTypes) {
		fTypeHandle= typeHandle;
		fName= name;
		fParameterTypes= argumentTypes;
	}

	public abstract void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator creator);

}
