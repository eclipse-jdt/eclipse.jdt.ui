/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import java.util.Collection;

/**
 * @deprecated Use org.eclipse.jdt.internal.corext.util.TypeInfoRequestor
 */
public class TypeInfoRequestor extends org.eclipse.jdt.internal.corext.util.TypeInfoRequestor {
		
	/**
	 * Constructs the TypeRefRequestor
	 * @param typesFound Will collect all TypeRef's found
	 */
	public TypeInfoRequestor(Collection typesFound) {
		super(typesFound);
	}
}