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

//TODO: tell that TypeHandles are unique and can be compared by == or equals()
public class TypeHandle {
	
	private String fTypeKey;
	private final String fQualifiedName;
	//TODO: type parameters, array component types
	
	TypeHandle(String typeKey, String qualifiedName) {
		fTypeKey= typeKey;
		fQualifiedName= qualifiedName;
	}
	
	public String getTypeKey() {
		return fTypeKey;
	}
	
	public String getQualifiedName() {
		return fQualifiedName;
	}
	
	//TODO: JLS3: 4.9 Subtyping. (Maybe on TypeHandleFactory)
	// isSupertype
	// isProperSupertype
	// isDirectSupertype
	
	
	
	public String toString() {
		return fQualifiedName;
	}
}
