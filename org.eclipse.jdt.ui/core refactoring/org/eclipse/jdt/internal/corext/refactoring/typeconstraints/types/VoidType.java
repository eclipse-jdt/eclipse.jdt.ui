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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.jdt.core.Signature;


public class VoidType extends TType {

	protected VoidType(TypeEnvironment environment) {
		super(environment, Signature.createTypeSignature("void", true)); //$NON-NLS-1$
	}

	public int getElementType() {
		return VOID_TYPE;
	}

	protected boolean doEquals(TType type) {
		return true;
	}

	protected boolean doCanAssignTo(TType lhs) {
		return false;
	}

	public String getName() {
		return "void"; //$NON-NLS-1$
	}

	public String getPrettySignature() {
		return getName();
	}
}
