/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

import java.util.Collection;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.search.ITypeNameRequestor;

public class TypeInfoRequestor implements ITypeNameRequestor {
	
	private Collection fTypesFound;
	
	/**
	 * Constructs the TypeRefRequestor
	 * @param typesFound Will collect all TypeRef's found
	 */
	public TypeInfoRequestor(Collection typesFound) {
		Assert.isNotNull(typesFound);
		fTypesFound= typesFound;
	}

	/* non java-doc
	 * @see ITypeNameRequestor#acceptInterface
	 */
	public void acceptInterface(char[] packageName, char[] typeName, char[][] enclosingTypeNames,String path) {
		fTypesFound.add(new TypeInfo(packageName, typeName, enclosingTypeNames, path, true));
	}

	/* non java-doc
	 * @see ITypeNameRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[][] enclosingTypeNames, String path) {
		fTypesFound.add(new TypeInfo(packageName, typeName, enclosingTypeNames, path, false));
	}
}