/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class PackageFragmentRootNamingPolicy implements INamingPolicy {
	
	public String isValidNewName(Object original, Object parent, String name) {
		IJavaProject project= (IJavaProject)parent;
		IPath p= project.getProject().getFullPath().append(name);
		try {
			if (project.findPackageFragmentRoot(p) != null)
				return ReorgMessages.getString("packageFragementRootNamingPolicy.duplicate"); //$NON-NLS-1$
		} catch (JavaModelException e) {
			return ReorgMessages.getString("packageFragementRootNamingPolicy.exception"); //$NON-NLS-1$
		}
		return null;
	}

	public boolean canReplace(Object original, Object parent, String newName) {
		IJavaProject project= (IJavaProject)parent;
		IPath p= project.getProject().getFullPath().append(newName);
		try {
			if (original.equals(project.findPackageFragmentRoot(p)))
				return false;
		} catch (JavaModelException e) {
		}
		return true;
	}
	
	public Object getElement(Object parent, String name) {
		IJavaProject project= (IJavaProject)parent;
		IPath p= project.getProject().getFullPath().append(name);
		try {
			return project.findPackageFragmentRoot(p);
		} catch (JavaModelException e) {
		}
		return null;
	}
}