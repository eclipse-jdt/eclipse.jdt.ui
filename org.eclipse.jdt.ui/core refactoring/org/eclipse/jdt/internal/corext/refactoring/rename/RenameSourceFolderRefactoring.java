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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;


public class RenameSourceFolderRefactoring extends Refactoring implements IRenameRefactoring {

	private IPackageFragmentRoot fSourceFolder;
	private String fNewName;
	
	private RenameSourceFolderRefactoring(IPackageFragmentRoot sourceFolder){
		Assert.isNotNull(sourceFolder); 
		fSourceFolder= sourceFolder;
		fNewName= fSourceFolder.getElementName();
	}
	
	public static RenameSourceFolderRefactoring create(IPackageFragmentRoot sourceFolder) throws JavaModelException{
		if (! isAvailable(sourceFolder))
			return null;
		return new RenameSourceFolderRefactoring(sourceFolder);
	}
	
	public static boolean isAvailable(IPackageFragmentRoot sourceFolder) throws JavaModelException{
		if (! Checks.isAvailable(sourceFolder))
			return false;
		
		if (sourceFolder.isArchive())
			return false;
		
		if (sourceFolder.isExternal())	
			return false;
			
		if (! sourceFolder.isConsistent())	
			return false;
		
		if (sourceFolder.getResource() instanceof IProject)
			return false;

		return true;
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		String message= RefactoringCoreMessages.getFormattedString("RenameSourceFolderRefactoring.rename", //$NON-NLS-1$
					new String[]{fSourceFolder.getElementName(), fNewName});
		return message;
	}
	
	public Object getNewElement() throws JavaModelException{
		IPackageFragmentRoot[] roots= fSourceFolder.getJavaProject().getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].getElementName().equals(fNewName))
				return roots[i];	
		}
		return null;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#setNewName(String)
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getNewName()
	*/
	public String getNewName(){
		return fNewName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fSourceFolder.getElementName();
	}
			
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);  //$NON-NLS-1$
		pm.done();
		return new RefactoringStatus();
	}

	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
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
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
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
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new RenameSourceFolderChange(fSourceFolder, fNewName);
		} finally{
			pm.done();
		}	
	}
}

