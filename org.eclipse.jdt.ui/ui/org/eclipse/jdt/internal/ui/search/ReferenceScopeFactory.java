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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;


public class ReferenceScopeFactory {

	private ReferenceScopeFactory() {
		// no instances, please
	}

	public static IJavaSearchScope createWorkspaceScope(boolean includeJRE) {
		try {
			return SearchEngine.createJavaSearchScope(getAllProjects(), JavaSearchScopeFactory.getSearchFlags(includeJRE));
		} catch (JavaModelException e) {
			return SearchEngine.createWorkspaceScope();
		}
	}

	private static IJavaProject[] getAllProjects() throws JavaModelException {
		IJavaModel model= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		return model.getJavaProjects();
	}
}
