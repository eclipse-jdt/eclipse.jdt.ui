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

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

public class TypeHandle {
	
	private String fTypeKey;
	//TODO: type parameters, array component types
	
	TypeHandle(String typeKey) {
		fTypeKey= typeKey;
	}
	
	public String getTypeKey() {
		return fTypeKey;
	}
	
	//TODO: JLS3: 4.9 Subtyping. (Maybe on BindingHandleFactory)
	// isSupertype
	// isProperSupertype
	// isDirectSupertype

}
