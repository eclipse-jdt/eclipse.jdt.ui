/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Robert M. Fuhrer (rfuhrer@watson.ibm.com), IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets;

import org.eclipse.jdt.core.IType;

public class TypeUniverseSet extends SubTypesOfSingleton {
	private static TypeUniverseSet sUniverse= null;

	public static TypeUniverseSet create() {
		if (sUniverse == null)
			sUniverse= new TypeUniverseSet();
		return sUniverse;
	}

	public static void clear() {
		sUniverse= null;
	}

	private TypeUniverseSet() {
		super(sJavaLangObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.SubTypesSet#contains(org.eclipse.jdt.core.IType)
	 */
	public boolean contains(IType t) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.SubTypesSet#containsAll(org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet s) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#addedTo(org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public TypeSet addedTo(TypeSet that) {
		return new TypeUniverseSet();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.SubTypesOfSingleton#makeClone()
	 */
	public TypeSet makeClone() {
		return this; // new TypeUniverseSet();
	}

	public String toString() {
		return "{ " + fID + ": <universe> }";  //$NON-NLS-1$//$NON-NLS-2$
	}
}
