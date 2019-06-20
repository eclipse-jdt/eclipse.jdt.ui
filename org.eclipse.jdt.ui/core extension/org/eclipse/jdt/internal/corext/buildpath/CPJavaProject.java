/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.buildpath;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

public class CPJavaProject {

	public static CPJavaProject createFromExisting(IJavaProject javaProject) throws CoreException {
		List<CPListElement> classpathEntries= ClasspathModifier.getExistingEntries(javaProject);
		return new CPJavaProject(javaProject, classpathEntries, javaProject.getOutputLocation());
    }

	private final IJavaProject fJavaProject;
    private final List<CPListElement> fCPListElements;
	private IPath fDefaultOutputLocation;

	public CPJavaProject(IJavaProject javaProject, List<CPListElement> cpListElements, IPath defaultOutputLocation) {
		fJavaProject= javaProject;
		fCPListElements= cpListElements;
		fDefaultOutputLocation= defaultOutputLocation;
    }

    public CPJavaProject createWorkingCopy() {
    	List<CPListElement> newList= new ArrayList<>(fCPListElements.size());
		for (CPListElement element : fCPListElements) {
			newList.add(element.copy());
		}
		return new CPJavaProject(fJavaProject, newList, fDefaultOutputLocation);
    }

    public CPListElement get(int index) {
    	return fCPListElements.get(index);
    }

    public IClasspathEntry[] getClasspathEntries() {
    	IClasspathEntry[] result= new IClasspathEntry[fCPListElements.size()];
    	int i= 0;
		for (CPListElement element : fCPListElements) {
			result[i]= element.getClasspathEntry();
			i++;
		}
	    return result;
    }

    public CPListElement getCPElement(CPListElement element) {
		return ClasspathModifier.getClasspathEntry(fCPListElements, element);
    }

    public List<CPListElement> getCPListElements() {
	    return fCPListElements;
    }

    public IPath getDefaultOutputLocation() {
	    return fDefaultOutputLocation;
    }

    public IJavaProject getJavaProject() {
		return fJavaProject;
    }

    public int indexOf(CPListElement element) {
		return fCPListElements.indexOf(element);
    }

    public void setDefaultOutputLocation(IPath path) {
    	fDefaultOutputLocation= path;
    }
}
