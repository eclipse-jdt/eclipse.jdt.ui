/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * A type info element that represent an unresolveable type. This can happen if
 * the search engine reports a type name that doesn't exist in the workspace.
 */
public class UnresolvedTypeInfo extends TypeInfo {
	
	private final String fPath;
	
	public UnresolvedTypeInfo(String pkg, String name, char[][] enclosingTypes, boolean isInterface, String path) {
		super(pkg, name, enclosingTypes, isInterface);
		fPath= path;
	}
	
	public int getElementType() {
		return TypeInfo.DEFAULT_TYPE_INFO;
	}
	
	protected String getPath() {
		return fPath;
	}
	
	public IPath getPackageFragmentRootPath() {
		return new Path(fPath);
	}
	
	protected IJavaElement getJavaElement(IJavaSearchScope scope) {
		return null;
	}
}