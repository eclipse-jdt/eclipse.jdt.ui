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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DeleteFromClasspathChange extends JDTChange {

	private final String fProjectHandle;
	private final IPath fPathToDelete;
	
	private IPath fPath;
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentRootPath;
	private int fEntryKind;
	
	public DeleteFromClasspathChange(IPackageFragmentRoot root) {
		this(root.getPath(), root.getJavaProject());
	}
	
	DeleteFromClasspathChange(IPath pathToDelete, IJavaProject project){
		Assert.isNotNull(pathToDelete);
		fPathToDelete= pathToDelete;
		fProjectHandle= project.getHandleIdentifier();
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// we have checked the .classpath file in the delete change.
		return super.isValid(pm, READ_ONLY | DIRTY);
	}
	
	public Change perform(IProgressMonitor pm)	throws CoreException {
		pm.beginTask(getName(), 1);
		try{
			IJavaProject project= getJavaProject();
			IClasspathEntry[] cp= project.getRawClasspath();
			IClasspathEntry[] newCp= new IClasspathEntry[cp.length-1];
			int i= 0; 
			int j= 0;
			while (j < newCp.length) {
				IClasspathEntry current= JavaCore.getResolvedClasspathEntry(cp[i]);
				if (current != null && toBeDeleted(current)) {
					i++;
					setDeletedEntryProperties(current);
				} 

				newCp[j]= cp[i];
				i++;
				j++;
			}
			
			IClasspathEntry last= JavaCore.getResolvedClasspathEntry(cp[cp.length - 1]);
			if (last != null && toBeDeleted(last))
				setDeletedEntryProperties(last);
				
			project.setRawClasspath(newCp, pm);
			
			return new AddToClasspathChange(getJavaProject(), fEntryKind, fPath, 
				fSourceAttachmentPath, fSourceAttachmentRootPath);
		} finally {
			pm.done();
		}
	}
	
	private boolean toBeDeleted(IClasspathEntry entry){
		if (entry == null) //safety net
			return false; 
		return fPathToDelete.equals(entry.getPath());
	}
	
	private void setDeletedEntryProperties(IClasspathEntry entry){
		fEntryKind= entry.getEntryKind();
		fPath= entry.getPath();
		fSourceAttachmentPath= entry.getSourceAttachmentPath();
		fSourceAttachmentRootPath= entry.getSourceAttachmentRootPath();
	}
	
	private IJavaProject getJavaProject(){
		return (IJavaProject)JavaCore.create(fProjectHandle);
	}
	
	public String getName() {
		return RefactoringCoreMessages.DeleteFromClassPathChange_remove + getJavaProject().getElementName(); 
	}

	public Object getModifiedElement() {
		return getJavaProject();
	}
}
