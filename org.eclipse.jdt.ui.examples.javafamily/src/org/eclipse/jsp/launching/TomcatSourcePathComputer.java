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
package org.eclipse.jsp.launching;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourcePathComputer;

/**
 * Computes the default source lookup path for a Tomcat launch config.
 * The source path is the same as for a Java project with the addition
 * of the folder containing the JSPs. 
 */
public class TomcatSourcePathComputer extends JavaSourcePathComputer {
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate#computeSourceContainers(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		ISourceContainer[] containers = super.computeSourceContainers(configuration, monitor);
		String root = configuration.getAttribute(TomcatLaunchDelegate.ATTR_WEB_APP_ROOT, (String)null);
		ISourceContainer folder = null;
		if (root != null) {
			IPath path = new Path(root);
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (resource != null && resource instanceof IFolder) {
				folder = new FolderSourceContainer((IFolder)resource, true);
			} 
		}	
		if (folder == null) {
			return containers;
		} else {
			ISourceContainer[] all = new ISourceContainer[containers.length + 1];
			System.arraycopy(containers, 0, all, 0, containers.length);
			all[containers.length] = folder;
			return all;
		}
	}
}
