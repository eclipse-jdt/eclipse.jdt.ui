/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

public interface IImportsStructure {

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added. An import is also not added if its container is 'java.lang' or the
	 * same package as the compilation unit.
	 * @param qualifiedTypeName The fully qualified name of the type to import
	 *        (dot separated)
	 * @return Retuns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */		
	String addImport(String qualifiedTypeName);
	

	/**
	 * Adds a new import declaration that is sorted in the structure using
	 * a best match algorithm. If an import already exists, the import is
	 * not added. An import is also not added if its container is 'java.lang' or the
	 * same package as the compilation unit.
	 * @param typeContainerName The name of the type container (package name or outer type name) to import
	 * @param typeName The type name of the type to import (can be '*' for imports-on-demand)
	 * @return Retuns the simple type name that can be used in the code or the
	 * fully qualified type name if an import conflict prevented the import.
	 */
	String addImport(String typeContainerName, String typeName);
	
}
