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


public class NullType extends Type {

	protected NullType(TypeEnvironment environment) {
		super(environment);
	}

	public int getElementType() {
		return NULL_TYPE;
	}

	protected boolean doEquals(Type type) {
		return true;
	}

	public String getPrettySignature() {
		return "null";  //$NON-NLS-1$
	}
	
	protected boolean doCanAssignTo(Type target) {
		return target.getElementType() != PRIMITIVE_TYPE;
	}
}
