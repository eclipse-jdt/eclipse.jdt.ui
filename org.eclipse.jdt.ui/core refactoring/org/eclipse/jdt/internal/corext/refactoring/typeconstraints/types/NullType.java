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


public class NullType extends TType {

	protected NullType(TypeEnvironment environment) {
		super(environment, "N"); //$NON-NLS-1$
	}

	public int getElementType() {
		return NULL_TYPE;
	}

	protected boolean doEquals(TType type) {
		return true;
	}

	public String getName() {
		return "null";  //$NON-NLS-1$
	}
	
	public String getPrettySignature() {
		return getName();
	}
	
	protected boolean doCanAssignTo(TType target) {
		int elementType= target.getElementType();
		return elementType != PRIMITIVE_TYPE && elementType != VOID_TYPE;
	}
}
