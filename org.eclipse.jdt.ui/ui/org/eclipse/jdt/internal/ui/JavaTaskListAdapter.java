/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class JavaTaskListAdapter implements ITaskListResourceAdapter {
	/*
	 * @see ITaskListResourceAdapter#getAffectedResource(IAdaptable)
	 */
	public IResource getAffectedResource(IAdaptable element) {
		IJavaElement java = (IJavaElement) element;
		IResource resource= java.getResource();
		if (resource != null)
			return resource; 
		
		ICompilationUnit cu= (ICompilationUnit) java.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (cu != null) {
			return JavaModelUtil.toOriginal(cu).getResource();
		}
		return null;
	 }
}

