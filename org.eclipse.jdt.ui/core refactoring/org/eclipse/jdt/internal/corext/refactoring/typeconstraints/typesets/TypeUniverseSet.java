/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Robert M. Fuhrer (rfuhrer@watson.ibm.com), IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;

public class TypeUniverseSet extends SubTypesOfSingleton {

	TypeUniverseSet(TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment.getJavaLangObject(), typeSetEnvironment);
	}

	@Override
	public boolean contains(TType t) {
		return true;
	}

	@Override
	public boolean containsAll(TypeSet s) {
		return true;
	}

	@Override
	public TypeSet addedTo(TypeSet that) {
		return this;
	}

	@Override
	public TypeSet makeClone() {
		return this; // new TypeUniverseSet();
	}

	@Override
	public String toString() {
		return "{ " + fID + ": <universe> }";  //$NON-NLS-1$//$NON-NLS-2$
	}
}
