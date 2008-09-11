/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.buildpath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

public class CPJavaProject {

	public static CPJavaProject createFromExisting(IJavaProject javaProject) throws CoreException {
		List classpathEntries= ClasspathModifier.getExistingEntries(javaProject);
		return new CPJavaProject(classpathEntries, javaProject.getOutputLocation());
    }

    private final List fCPListElements;
	private IPath fDefaultOutputLocation;

	public CPJavaProject(List cpListElements, IPath defaultOutputLocation) {
		fCPListElements= cpListElements;
		fDefaultOutputLocation= defaultOutputLocation;
    }

    public CPJavaProject createWorkingCopy() {
    	List newList= new ArrayList(fCPListElements.size());
    	for (Iterator iterator= fCPListElements.iterator(); iterator.hasNext();) {
	        CPListElement element= (CPListElement)iterator.next();
	        newList.add(element.copy());
        }
	    return new CPJavaProject(newList, fDefaultOutputLocation);
    }

    public CPListElement get(int index) {
    	return (CPListElement)fCPListElements.get(index);
    }

    public IClasspathEntry[] getClasspathEntries() {
    	IClasspathEntry[] result= new IClasspathEntry[fCPListElements.size()];
    	int i= 0;
    	for (Iterator iterator= fCPListElements.iterator(); iterator.hasNext();) {
	        CPListElement element= (CPListElement)iterator.next();
	        result[i]= element.getClasspathEntry();
	        i++;
        }
	    return result;
    }

    public CPListElement getCPElement(CPListElement element) {
		return ClasspathModifier.getClasspathEntry(fCPListElements, element);
    }

    public List getCPListElements() {
	    return fCPListElements;
    }

    public IPath getDefaultOutputLocation() {
	    return fDefaultOutputLocation;
    }

    public IJavaProject getJavaProject() {
	    return ((CPListElement)fCPListElements.get(0)).getJavaProject();
    }

    public int indexOf(CPListElement element) {
		return fCPListElements.indexOf(element);
    }

    public void setDefaultOutputLocation(IPath path) {
    	fDefaultOutputLocation= path;
    }
}
