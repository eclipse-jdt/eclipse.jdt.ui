/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class PackageFragmentRootNamingPolicy implements INamingPolicy {
	private static final String PREFIX= "reorg_policy.package_fragment.";
	private static final String ERROR_DUPLICATE= "duplicate";
	private static final String ERROR_EXCEPTION= "exception";
	
	public String isValidNewName(Object original, Object parent, String name) {
		IJavaProject project= (IJavaProject)parent;
		IPath p= project.getProject().getFullPath().append(name);
		try {
			if (project.findPackageFragmentRoot(p) != null)
				return JavaPlugin.getResourceString(PREFIX+ERROR_DUPLICATE);
		} catch (JavaModelException e) {
			return JavaPlugin.getResourceString(PREFIX+ERROR_EXCEPTION);
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