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

package org.eclipse.jdt.text.tests.performance;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class ResourceTestHelper {

	public static void replicate(String src, String destPrefix, String destSuffix, int n) throws CoreException {
		for (int i= 0; i < n; i++)
			copy(src, destPrefix + i + destSuffix);
	}

	public static void copy(String src, String dest) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IFile file= root.getFile(new Path(src));
		file.copy(new Path(dest), true, null);
	}

	public static void delete(String file) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		root.getFile(new Path(file)).delete(true, null);
	}

	public static IFile findFile(String path) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		return root.getFile(new Path(path));
	}

	public static IFile[] findFiles(String prefix, String suffix, int i, int n) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		List files= new ArrayList(n - i);
		for (int j= i; j < i + n; j++) {
			String path= root.getLocation().toString() + "/" + prefix + j + suffix;
			files.add(findFile(path));
		}
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}
}
