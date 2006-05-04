/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * A <tt>JarFileEntryTypeInfo</tt> represents a type in a Jar file.
 */
public class JarFileEntryTypeInfo extends TypeInfo {
	
	private final String fJar;
	private final String fFileName;
	private final String fExtension;
	
	public JarFileEntryTypeInfo(String pkg, String name, char[][] enclosingTypes, int modifiers, String jar, String fileName, String extension) {
		super(pkg, name, enclosingTypes, modifiers);
		fJar= jar;
		fFileName= fileName;
		fExtension= extension;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!JarFileEntryTypeInfo.class.equals(obj.getClass()))
			return false;
		JarFileEntryTypeInfo other= (JarFileEntryTypeInfo)obj;
		return doEquals(other) && fJar.equals(other.fJar) && 
			fFileName.equals(other.fFileName) && fExtension.equals(other.fExtension);
	}
	
	public int getElementType() {
		return TypeInfo.JAR_FILE_ENTRY_TYPE_INFO;
	}
	
	public String getJar() {
		return fJar;
	}
	
	public String getFileName() {
		return fFileName;
	}
	
	public String getExtension() {
		return fExtension;
	}
	
	protected IJavaElement getContainer(IJavaSearchScope scope) throws JavaModelException {
		IJavaModel jmodel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IPath[] enclosedPaths= scope.enclosingProjectsAndJars();

		for (int i= 0; i < enclosedPaths.length; i++) {
			IPath curr= enclosedPaths[i];
			if (curr.segmentCount() == 1) {
				IJavaProject jproject= jmodel.getJavaProject(curr.segment(0));
				IPackageFragmentRoot root= jproject.getPackageFragmentRoot(fJar);
				if (root.exists()) {
					return findElementInRoot(root);
				}
			}
		}
		List paths= Arrays.asList(enclosedPaths);
		IJavaProject[] projects= jmodel.getJavaProjects();
		for (int i= 0; i < projects.length; i++) {
			IJavaProject jproject= projects[i];
			if (!paths.contains(jproject.getPath())) {
				IPackageFragmentRoot root= jproject.getPackageFragmentRoot(fJar);
				if (root.exists()) {
					return findElementInRoot(root);
				}
			}
		}
		return null;
	}
	
	private IJavaElement findElementInRoot(IPackageFragmentRoot root) {
		IJavaElement res;
		IPackageFragment frag= root.getPackageFragment(getPackageName());
		String extension= getExtension();
		String fullName= getFileName() + '.' + extension;
		
		if ("class".equals(extension)) { //$NON-NLS-1$
			res=  frag.getClassFile(fullName);
		} else if (JavaCore.isJavaLikeFileName(fullName)) {
			res=  frag.getCompilationUnit(fullName);
		} else {
			return null;
		}
		if (res.exists()) {
			return res;
		}
		return null;
	}
	
	public IPath getPackageFragmentRootPath() {
		return new Path(fJar);
	}
	
	public String getPackageFragmentRootName() {
		// we can't remove the '/' since the jar can be external.
		return fJar;
	}
		
	public String getPath() {
		StringBuffer result= new StringBuffer(fJar);
		result.append(IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR);
		getElementPath(result);
		return result.toString();
	}
	
	public long getContainerTimestamp() {
		// First try internal Jar
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IPath path= new Path(fJar);
		IResource resource= root.findMember(path);
		IFileInfo info= null;
		IJavaElement element= null;
		if (resource != null && resource.exists()) {
			URI location= resource.getLocationURI();
			if (location != null) {
				try {
					info= EFS.getStore(location).fetchInfo();
					if (info.exists()) {
						element= JavaCore.create(resource);
						// The exist test for external jars is expensive due to
						// JDT/Core. So do the test here since we know that the
						// Java element points to an internal Jar. 
						if (element != null && !element.exists())
							element= null;
					}
				} catch (CoreException e) {
					// Fall through
				}
			}
		} else {
			info= EFS.getLocalFileSystem().getStore(Path.fromOSString(fJar)).fetchInfo();
			if (info.exists()) {
				element= getPackageFragementRootForExternalJar();
			}
		}
		if (info != null && info.exists() && element != null) {
			return info.getLastModified();
		}
		return IResource.NULL_STAMP;
	}
	
	public boolean isContainerDirty() {
		return false;
	}
		
	private void getElementPath(StringBuffer result) {
		String pack= getPackageName();
		if (pack != null && pack.length() > 0) {
			result.append(pack.replace(TypeInfo.PACKAGE_PART_SEPARATOR, TypeInfo.SEPARATOR));
			result.append(TypeInfo.SEPARATOR);
		}
		result.append(getFileName());
		result.append('.');
		result.append(getExtension());
	}
	
	private IPackageFragmentRoot getPackageFragementRootForExternalJar() {
		try {
			IJavaModel jmodel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			IJavaProject[] projects= jmodel.getJavaProjects();
			for (int i= 0; i < projects.length; i++) {
				IJavaProject project= projects[i];
				IPackageFragmentRoot root= project.getPackageFragmentRoot(fJar);
				// Cheaper check than calling root.exists().
				if (project.isOnClasspath(root))
					return root;
			}
		} catch (JavaModelException e) {
			// Fall through
		}
		return null;
	}
}