/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

public interface IImportsStructure {

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added. An import is also not added if its container is 'java.lang' or the
	 * same package as the compilation unit.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 *        (dot separated)
	 */		
	void addImport(String qualifiedTypeName);
	

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added. An import is also not added if its container is 'java.lang' or the
	 * same package as the compilation unit.
	 * @param packageName The package name of the type to import
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 */			
	void addImport(String packageName, String typeName);
	
}
