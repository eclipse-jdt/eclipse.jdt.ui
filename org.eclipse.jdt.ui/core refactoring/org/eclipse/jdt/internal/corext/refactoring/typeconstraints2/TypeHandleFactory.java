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

import java.util.HashMap;

import org.eclipse.jdt.core.dom.ITypeBinding;


public class TypeHandleFactory {

	HashMap fKeyToTypeHandle; // WeakHashMap wouldn't work if TypeHandle holds a reference to key
	
	public TypeHandleFactory() {
		fKeyToTypeHandle= new HashMap();
	}
	
	public TypeHandle getTypeHandle(ITypeBinding typeBinding) {
		String key= typeBinding.getKey();
		TypeHandle stored= (TypeHandle) fKeyToTypeHandle.get(key);
		if (stored != null)
			return stored;
		
		//TODO: create supertype, array component, and type parameter TypeHandles
		stored= new TypeHandle(key);
		fKeyToTypeHandle.put(key, stored);
		return stored;
	}

}
