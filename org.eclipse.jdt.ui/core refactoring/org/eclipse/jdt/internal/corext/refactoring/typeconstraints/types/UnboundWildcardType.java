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

public final class UnboundWildcardType extends WildcardType {

	protected UnboundWildcardType(TypeEnvironment environment) {
		super(environment);
	}

	public int getElementType() {
		return UNBOUND_WILDCARD_TYPE;
	}
	
	protected boolean doCanAssignTo(Type lhs) {
		switch(lhs.getElementType()) {
			case UNBOUND_WILDCARD_TYPE:
				return true;
			case EXTENDS_WILDCARD_TYPE:
				return ((ExtendsWildcardType)lhs).getBound().isJavaLangObject();
			default:
				return false;
		}
	}
	
	protected boolean checkBound(Type rhs) {
		return true;
	}
	
	public String getPrettySignature() {
		return "?"; //$NON-NLS-1$
	}
}
