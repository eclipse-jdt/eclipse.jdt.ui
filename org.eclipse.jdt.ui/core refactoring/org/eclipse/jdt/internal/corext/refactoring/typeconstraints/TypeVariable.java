/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.Type;

public class TypeVariable extends ConstraintVariable {

	private final Type fType;
	
	public TypeVariable(Type type){
		super(type.resolveBinding());
		fType= type;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fType.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (! super.equals(obj))
			return false;
		if (! (obj instanceof TypeVariable))
			return false;
		TypeVariable other= (TypeVariable)obj;
		return TypeBindings.isEqualTo(fType.resolveBinding(), other.fType.resolveBinding());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (fType.resolveBinding() == null)
			return super.hashCode();
		return super.hashCode() ^ TypeBindings.hashCode(fType.resolveBinding());
	}

	public Type getType() {
		return fType;
	}

}
