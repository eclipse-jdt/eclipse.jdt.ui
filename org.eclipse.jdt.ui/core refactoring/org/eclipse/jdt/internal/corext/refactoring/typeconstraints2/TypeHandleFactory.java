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

	private final static TypeHandle[] EMPTY= { };
	
	HashMap/*<String, TypeHandle>*/ fKeyToTypeHandle; // WeakHashMap wouldn't work if TypeHandle holds a reference to key
	
	public TypeHandleFactory() {
		fKeyToTypeHandle= new HashMap();
	}
	
	public TypeHandle getTypeHandle(ITypeBinding typeBinding) {
		String key= typeBinding.getKey();
		TypeHandle stored= (TypeHandle) fKeyToTypeHandle.get(key);
		if (stored != null)
			return stored;
		
		//TODO: special cases with Object, Cloneable, Serializable, ...; array components; type parameters; ...
		if ((typeBinding.isClass() || typeBinding.isInterface())
				&& ! typeBinding.isArray()) {
			TypeHandle[] directSupertypes= getDirectSuperTypes(typeBinding);
			// Have to check again, since type handle could have been stored in the meantime!
			stored= (TypeHandle) fKeyToTypeHandle.get(key);
			if (stored != null)
				return stored;
			stored= new TypeHandle(key, typeBinding.getQualifiedName(), directSupertypes);
		} else {
			stored= new TypeHandle(key, typeBinding.getQualifiedName(), EMPTY);
		}
		fKeyToTypeHandle.put(key, stored);
		return stored;
	}

	private TypeHandle[] getDirectSuperTypes(ITypeBinding typeBinding) {
		ITypeBinding superclass= typeBinding.getSuperclass();
		ITypeBinding[] interfaces= typeBinding.getInterfaces();
		if (superclass == null) {
			if (interfaces.length == 0) {
				return EMPTY;
			} else {
				TypeHandle[] result= new TypeHandle[interfaces.length];
				for (int i= 0; i < interfaces.length; i++)
					result[i]= getTypeHandle(interfaces[i]);
				return result;
			}
		} else {
			if (interfaces.length == 0) {
				return new TypeHandle[] { getTypeHandle(superclass) };
			} else {
				TypeHandle[] result= new TypeHandle[interfaces.length + 1];
				result[0]= getTypeHandle(superclass);
				for (int i= 0; i < interfaces.length; i++)
					result[i + 1]= getTypeHandle(interfaces[i]);
				return result;
			}
		}
	}

}
