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

/**
 * {@link TypeHandle}s from the same {@link TypeHandleFactory} are unique
 * and can be compared with == instead of equals().
 */
public final class TypeHandle {
	
	private String fTypeKey;
	private final String fQualifiedName;
	private final TypeHandle[] fDirectSupertypes;
	//TODO: type parameters, array component types
	
	/*package*/ TypeHandle(String typeKey, String qualifiedName, TypeHandle[] directDupertypes) {
		fTypeKey= typeKey;
		fQualifiedName= qualifiedName;
		fDirectSupertypes= directDupertypes;
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

	public String getSimpleName() {
		int dot= fQualifiedName.lastIndexOf('.');
		return fQualifiedName.substring(dot + 1);
	}
	
	/**
	 * Subtype test.
	 * @param targetTypeCandidate the type of the target
	 * @return <code>true</code> iff 
	 * <pre>
	 * 		'this' x; 
	 * 		'targetTypeCandidate' target= x;
	 * </pre>
	 * is valid; <code>false</code> otherwise
	 */
	public boolean canAssignTo(TypeHandle targetTypeCandidate) {
		if (this == targetTypeCandidate)
			return true;

		for (int i= 0; i < fDirectSupertypes.length; i++)
			if (fDirectSupertypes[i].canAssignTo(targetTypeCandidate))
				return true;

		return false;
	}
}
