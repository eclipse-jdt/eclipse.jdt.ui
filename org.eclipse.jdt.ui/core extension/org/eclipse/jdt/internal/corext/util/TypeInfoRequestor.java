/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;


import org.eclipse.jdt.core.search.TypeNameRequestor;


public class TypeInfoRequestor extends TypeNameRequestor {
	
	private Collection fTypesFound;
	private TypeInfoFactory fFactory;
	
	/**
	 * Constructs the TypeRefRequestor
	 * @param typesFound Will collect all TypeRef's found
	 */
	public TypeInfoRequestor(Collection typesFound) {
		Assert.isNotNull(typesFound);
		fTypesFound= typesFound;
		fFactory= new TypeInfoFactory();
	}
	
	protected boolean inScope(char[] packageName, char[] typeName) {
		return !TypeFilter.isFiltered(packageName, typeName);
	}

	public void acceptType(int modifiers, char[] packageName, char[] typeName, char[][] enclosingTypeNames, String path) {
		if (inScope(packageName, typeName)) {
			fTypesFound.add(fFactory.create(packageName, typeName, enclosingTypeNames, modifiers, path));
		}
	}
}
