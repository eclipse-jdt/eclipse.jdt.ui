/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * A <tt>JarFileEntryTypeInfo</tt> represents a type in a Jar file.
 */
public class JarFileEntryTypeInfo extends TypeInfo {
	
	private final String fJar;
	private final String fFile;
	private final String fExtension;
	
	public JarFileEntryTypeInfo(String pkg, String name, char[][] enclosingTypes, boolean isInterface, String jar, String file, String extension) {
		super(pkg, name, enclosingTypes, isInterface);
		fJar= jar;
		fFile= file;
		fExtension= extension;
	}
	
	public int getElementType() {
		return TypeInfo.JAR_FILE_ENTRY_TYPE_INFO;
	}
	
	public String getJar() {
		return fJar;
	}
	
	public String getFileName() {
		return fFile;
	}
	
	public String getExtension() {
		return fExtension;
	}
	
	protected IJavaElement getJavaElement(IJavaSearchScope scope) throws JavaModelException {
		IJavaModel jmodel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IPath[] enclosedPaths= scope.enclosingProjectsAndJars();
		IPath elementPath= new Path(getElementPath());
		for (int i= 0; i < enclosedPaths.length; i++) {
			IPath curr= enclosedPaths[i];
			if (curr.segmentCount() == 1) {
				IJavaProject jproject= jmodel.getJavaProject(curr.segment(0));
				IPackageFragmentRoot root= jproject.getPackageFragmentRoot(fJar);
				if (root.exists()) {
					return jproject.findElement(elementPath);
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
					return jproject.findElement(elementPath);
				}
			}
		}
		return null;
	}
	
	public IPath getPackageFragmentRootPath() {
		return new Path(fJar);
	}
		
	public String getPath() {
		StringBuffer result= new StringBuffer(fJar);
		result.append(IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR);
		getElementPath(result);
		return result.toString();
	}
	
	private String getElementPath() {
		StringBuffer buffer= new StringBuffer();
		getElementPath(buffer);
		return buffer.toString();
	}
	
	private void getElementPath(StringBuffer result) {
		if (fPackage != null && fPackage.length() > 0) {
			result.append(fPackage.replace(TypeInfo.PACKAGE_PART_SEPARATOR, TypeInfo.SEPARATOR));
			result.append(TypeInfo.SEPARATOR);
		}
		result.append(fFile);
		result.append('.');
		result.append(fExtension);
	}
}
