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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class AddToClasspathChange extends JDTChange {
	
	private IJavaProject fProjectHandle;
	private IClasspathEntry fEntryToAdd;
	
	public AddToClasspathChange(IJavaProject project, IClasspathEntry entryToAdd) {
		fProjectHandle= project;
		fEntryToAdd= entryToAdd;
	}
	
	public AddToClasspathChange(IJavaProject project, String sourceFolderName){
		this(project, JavaCore.newSourceEntry(project.getPath().append(sourceFolderName)));
	}
	
	/**
	 * Adds a new project class path entry to the project.
	 * @param project
	 * @param newProjectEntry (must be absolute <code>IPath</code>)
	 */
	public AddToClasspathChange(IJavaProject project, IPath newProjectEntry){
		this(project, JavaCore.newProjectEntry(newProjectEntry));
	}
	
	public AddToClasspathChange(IJavaProject project, int entryKind, IPath path, IPath sourceAttachmentPath, IPath sourceAttachmentRootPath){
		this(project, createNewClasspathEntry(entryKind, path, sourceAttachmentPath, sourceAttachmentRootPath));
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// .classpath file will be handled by JDT/Core.
		return super.isValid(pm, READ_ONLY | DIRTY);
	}
	
	public Change perform(IProgressMonitor pm) throws CoreException {
		pm.beginTask(getName(), 1);
		try {
			if (validateClasspath()) {
				getJavaProject().setRawClasspath(getNewClasspathEntries(), new SubProgressMonitor(pm, 1));
				IPath classpathEntryPath= JavaCore.getResolvedClasspathEntry(fEntryToAdd).getPath();
				return new DeleteFromClasspathChange(classpathEntryPath, getJavaProject());
			} else {
				return new NullChange();
			}
		} finally {
			pm.done();
		}		
	}
	
	public boolean validateClasspath() throws JavaModelException {
		IJavaProject javaProject= getJavaProject();
		IPath outputLocation= javaProject.getOutputLocation();
		IClasspathEntry[] newClasspathEntries= getNewClasspathEntries();
		return JavaConventions.validateClasspath(javaProject, newClasspathEntries, outputLocation).isOK();
	}
	
	private IClasspathEntry[] getNewClasspathEntries() throws JavaModelException{
		IClasspathEntry[] entries= getJavaProject().getRawClasspath();
		List cp= new ArrayList(entries.length + 1);
		cp.addAll(Arrays.asList(entries));
		cp.add(fEntryToAdd);
		return (IClasspathEntry[])cp.toArray(new IClasspathEntry[cp.size()]);
	}
	
	private static IClasspathEntry createNewClasspathEntry(int kind, IPath path, IPath sourceAttach, IPath sourceAttachRoot){
		switch(kind){
			case IClasspathEntry.CPE_LIBRARY:
				return JavaCore.newLibraryEntry(path, sourceAttach, sourceAttachRoot);
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(path);
			case IClasspathEntry.CPE_SOURCE:
				return JavaCore.newSourceEntry(path);
			case IClasspathEntry.CPE_VARIABLE:
				return JavaCore.newVariableEntry(path, sourceAttach, sourceAttachRoot);	
			case IClasspathEntry.CPE_CONTAINER:
				return JavaCore.newContainerEntry(path);	
			default:
				Assert.isTrue(false);
				return null;	
		}
	}
	
	private IJavaProject getJavaProject(){
		return fProjectHandle;
	}

	public String getName() {
		return RefactoringCoreMessages.AddToClasspathChange_add + getJavaProject().getElementName(); 
 
	}

	public Object getModifiedElement() {
		return getJavaProject();
	}
	
	public IClasspathEntry getClasspathEntry() {
		return fEntryToAdd;
	}
}
