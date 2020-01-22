/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;


public class ResourceUtil {

	private ResourceUtil(){
	}

	public static IFile[] getFiles(ICompilationUnit[] cus) {
		List<IResource> files= new ArrayList<>(cus.length);
		for (ICompilationUnit cu : cus) {
			IResource resource= cu.getResource();
			if (resource != null && resource.getType() == IResource.FILE)
				files.add(resource);
		}
		return files.toArray(new IFile[files.size()]);
	}

	public static IFile getFile(ICompilationUnit cu) {
		IResource resource= cu.getResource();
		if (resource != null && resource.getType() == IResource.FILE)
			return (IFile)resource;
		else
			return null;
	}

	//----- other ------------------------------

	public static IResource getResource(Object o){
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IJavaElement)
			return getResource((IJavaElement)o);
		return null;
	}

	private static IResource getResource(IJavaElement element){
		if (element.getElementType() == IJavaElement.COMPILATION_UNIT)
			return ((ICompilationUnit) element).getResource();
		else if (element instanceof IOpenable)
			return element.getResource();
		else
			return null;
	}
}
