/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ITypeRoot;

import org.eclipse.jdt.internal.corext.util.Strings;


public class BasicElementLabels {
	
	public static String getPathLabel(IPath path, boolean isOSPath) {
		String label;
		if (isOSPath) {
			label= path.toOSString();
		} else {
			label= path.makeRelative().toString();
		}
		return Strings.markLTR(label);
	}
	
	public static String getFilePattern(String name) {
		return Strings.markLTR(name, "*.?"); //$NON-NLS-1$
	}
		
	public static String getFileName(String name) {
		return Strings.markLTR(name);
	}
	
	public static String getURLPart(String name) {
		return Strings.markLTR(name, ":@?-"); //$NON-NLS-1$
	}
	
	public static String getResourceName(IResource resource) {
		return Strings.markLTR(resource.getName());
	}

	public static String getFileName(ITypeRoot typeRoot) {
		return Strings.markLTR(typeRoot.getElementName());
	}
	
	public static String getJavaElementName(String name) {
		return Strings.markLTR(name, "<>()?,");
	}
	
	public static String getVersionName(String name) {
		return Strings.markLTR(name); 
	}
}
