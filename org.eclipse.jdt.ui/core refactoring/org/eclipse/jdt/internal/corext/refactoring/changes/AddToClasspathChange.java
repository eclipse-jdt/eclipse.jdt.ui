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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

public class AddToClasspathChange extends Change {
	
	private String fProjectHandle;
	private int fEntryKind;
	private IPath fPath;
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentRootPath;
	
	public AddToClasspathChange(IJavaProject project, String sourceFolderName){
		fProjectHandle= project.getHandleIdentifier();
		fEntryKind= IClasspathEntry.CPE_SOURCE;
		fPath= project.getProject().getFullPath().append(sourceFolderName);
	}
	
	/**
	 * Adds a new project class path entry to the project.
	 * @param project
	 * @param newProjectEntry (must be absolute <code>IPath</code>)
	 */
	public AddToClasspathChange(IJavaProject project, IPath newProjectEntry){
		Assert.isTrue(newProjectEntry.isAbsolute());
		fProjectHandle= project.getHandleIdentifier();
		fEntryKind= IClasspathEntry.CPE_PROJECT;
		fPath= newProjectEntry;
	}
	
	public AddToClasspathChange(IJavaProject project, int entryKind, IPath path, IPath sourceAttachmentPath, IPath sourceAttachmentRootPath){
		fProjectHandle= project.getHandleIdentifier();
		fEntryKind= entryKind;
		fPath=path;
		fSourceAttachmentPath= sourceAttachmentPath;
		fSourceAttachmentRootPath= sourceAttachmentRootPath;
	}
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws ChangeAbortException, CoreException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			if (!entryAlreadyExists())
				getJavaProject().setRawClasspath(getNewClasspathEntries(), new SubProgressMonitor(pm, 1));
			else
				setActive(false);	
		}finally{
			pm.done();
		}		
	}
	
	private boolean entryAlreadyExists() throws JavaModelException{
		return Arrays.asList(getJavaProject().getRawClasspath()).contains(createNewClasspathEntry());
	}
	
	private IClasspathEntry[] getNewClasspathEntries() throws JavaModelException{
		IClasspathEntry[] entries= getJavaProject().getRawClasspath();
		List cp= new ArrayList(entries.length + 1);
		cp.addAll(Arrays.asList(entries));
		cp.add(createNewClasspathEntry());
		return (IClasspathEntry[])cp.toArray(new IClasspathEntry[cp.size()]);
	}
	
	private IClasspathEntry createNewClasspathEntry(){
		switch(fEntryKind){
			case IClasspathEntry.CPE_LIBRARY:
				return JavaCore.newLibraryEntry(fPath, fSourceAttachmentPath, fSourceAttachmentRootPath);
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(fPath);
			case IClasspathEntry.CPE_SOURCE:
				return JavaCore.newSourceEntry(fPath);
			case IClasspathEntry.CPE_VARIABLE:
				return JavaCore.newVariableEntry(fPath, fSourceAttachmentPath, fSourceAttachmentRootPath);	
			case IClasspathEntry.CPE_CONTAINER:
				return JavaCore.newContainerEntry(fPath);	
			default:
				Assert.isTrue(false);
				return null;	
		}
	}
	
	private IJavaProject getJavaProject(){
		return (IJavaProject)JavaCore.create(fProjectHandle);
	}

	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();

		IPath classpathEntryPath= JavaCore.getResolvedClasspathEntry(createNewClasspathEntry()).getPath();
		return new DeleteFromClasspathChange(classpathEntryPath, getJavaProject());
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("AddToClasspathChange.add") + getJavaProject().getElementName(); //$NON-NLS-1$
 
	}

	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getJavaProject();
	}
}

