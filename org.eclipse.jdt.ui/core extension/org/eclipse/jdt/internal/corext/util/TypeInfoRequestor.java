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

	/* non java-doc
	 * @see ITypeNameRequestor#acceptInterface
	 */
	public void acceptInterface(char[] packageName, char[] typeName, char[][] enclosingTypeNames,String path) {
		fTypesFound.add(fFactory.create(packageName, typeName, enclosingTypeNames, true, path));
	}

	/* non java-doc
	 * @see ITypeNameRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[][] enclosingTypeNames, String path) {
		fTypesFound.add(fFactory.create(packageName, typeName, enclosingTypeNames, false, path));
	}
	
}