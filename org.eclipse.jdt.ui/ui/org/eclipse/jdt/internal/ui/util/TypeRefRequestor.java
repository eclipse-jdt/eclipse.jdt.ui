package org.eclipse.jdt.internal.ui.util;

import java.util.Collection;

import org.eclipse.jdt.core.search.ITypeNameRequestor;


public class TypeRefRequestor implements ITypeNameRequestor {
	
	private Collection fTypesFound;
	
	/**
	 * Constructs the TypeRefRequestor
	 * @param typesFound Will collect all TypeRef's found
	 */
	public TypeRefRequestor(Collection typesFound) {
		fTypesFound= typesFound;
	}

	/**
	 * @see ITypeNameRequestor#acceptInterface
	 */
	public void acceptInterface(char[] packageName, char[] typeName, char[][] enclosingTypeNames,String path) {
		fTypesFound.add(new TypeRef(packageName, typeName, enclosingTypeNames, path, true));
	}

	/**
	 * @see ITypeNameRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[][] enclosingTypeNames, String path) {
		fTypesFound.add(new TypeRef(packageName, typeName, enclosingTypeNames, path, false));
	}
}