/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

/**
 * @deprecated Use org.eclipse.jdt.internal.corext.util.TypeInfo
 */
public class TypeInfo extends org.eclipse.jdt.internal.corext.util.TypeInfo {

	public TypeInfo(char[] pkg, char[] name, char[][] enclosingTypes, String path, boolean isInterface) {
		super(pkg, name, enclosingTypes, path, isInterface);
	}
	
}