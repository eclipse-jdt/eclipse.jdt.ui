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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.RenameProcessor;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;


public class RenameSourceFolderProcessor extends RenameProcessor {

	private IPackageFragmentRoot fSourceFolder;
	
	//---- IRefactoringProcessor ---------------------------------------------------

	public void initialize(Object[] elements) {
		Assert.isTrue(elements != null && elements.length == 1);
		Object element= elements[0];
		if (!(element instanceof IPackageFragmentRoot))
			return;
		fSourceFolder= (IPackageFragmentRoot)element;
		setNewElementName(fSourceFolder.getElementName());
	}
	
	public boolean isAvailable() throws CoreException {
		if (fSourceFolder == null)
			return false;
		if (! Checks.isAvailable(fSourceFolder))
			return false;
		
		if (fSourceFolder.isArchive())
			return false;
		
		if (fSourceFolder.isExternal())	
			return false;
			
		if (! fSourceFolder.isConsistent())	
			return false;
		
		if (fSourceFolder.getResource() instanceof IProject)
			return false;

		return true;
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString(
			"RenameSourceFolderRefactoring.rename", //$NON-NLS-1$
			new String[]{fSourceFolder.getElementName(), fNewElementName});
	}
	
	public IProject[] getAffectedProjects() throws CoreException {
		return JavaProcessors.computeScope(fSourceFolder);
	}
	
	public Object[] getElements() {
		return new Object[] {fSourceFolder};
	}

	public IResourceModifications getResourceModifications() throws CoreException {
		ResourceModifications result= new ResourceModifications();
		result.setRename(fSourceFolder.getResource(), getNewElementName());
		return result;		
	}
		 
	public Object getNewElement() throws CoreException {
		IPackageFragmentRoot[] roots= fSourceFolder.getJavaProject().getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].getElementName().equals(fNewElementName))
				return roots[i];	
		}
		return null;
	}
	
	//---- IRenameProcessor ----------------------------------------------
	
	public String getCurrentElementName() {
		return fSourceFolder.getElementName();
	}
			
	public RefactoringStatus checkActivation() throws CoreException {
		return new RefactoringStatus();
	}

	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		if (! newName.trim().equals(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameSourceFolderRefactoring.blank")); //$NON-NLS-1$
		
		IContainer c= 	fSourceFolder.getResource().getParent();
		if (! c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameSourceFolderRefactoring.invalid_name")); //$NON-NLS-1$
		
		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, IResource.FOLDER));
		if (result.hasFatalError())
			return result;		
				
		result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), IResource.FOLDER)));		
		if (result.hasFatalError())
			return result;
			
		IJavaProject project= fSourceFolder.getJavaProject();
		IPath p= project.getProject().getFullPath().append(newName);
		if (project.findPackageFragmentRoot(p) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameSourceFolderRefactoring.already_exists")); //$NON-NLS-1$
		
		if (project.getProject().findMember(new Path(newName)) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameSourceFolderRefactoring.alread_exists")); //$NON-NLS-1$
		return result;		
	}
	
	private String createNewPath(String newName) {
		return fSourceFolder.getPath().removeLastSegments(1).append(newName).toString();
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}		
	}
	
	//-- changes

	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new ValidationStateChange(
				new RenameSourceFolderChange(fSourceFolder, fNewElementName));
		} finally{
			pm.done();
		}	
	}
}

