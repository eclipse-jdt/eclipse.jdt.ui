/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

class Utils {
	
	//no instances
	private Utils(){
	}
	
	static IPath getResourcePath(IResource resource){
		return resource.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	static IFile getFile(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	}
	
	static IFolder getFolder(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
	}
	
	static IProject getProject(IPath path){
		return (IProject)ResourcesPlugin.getWorkspace().getRoot().findMember(path);
	}
	
}

