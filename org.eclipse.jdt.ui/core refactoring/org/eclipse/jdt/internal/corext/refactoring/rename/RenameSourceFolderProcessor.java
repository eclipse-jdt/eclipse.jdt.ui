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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.util.Messages;

public class RenameSourceFolderProcessor extends JavaRenameProcessor {
	
	private static final String ATTRIBUTE_PATH= "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$
	
	private IPackageFragmentRoot fSourceFolder;

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameSourceFolderProcessor"; //$NON-NLS-1$

	/**
	 * Creates a new rename source folder processor.
	 * @param root the package fragment root, or <code>null</code> if invoked by scripting
	 */
	public RenameSourceFolderProcessor(IPackageFragmentRoot root) {
		fSourceFolder= root;
		if (root != null)
			setNewElementName(root.getElementName());
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fSourceFolder);
	}
	
	public String getProcessorName() {
		return Messages.format(
			RefactoringCoreMessages.RenameSourceFolderRefactoring_rename, 
			new String[]{fSourceFolder.getElementName(), getNewElementName()});
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fSourceFolder);
	}
	
	public Object[] getElements() {
		return new Object[] {fSourceFolder};
	}

	public Object getNewElement() throws CoreException {
		IPackageFragmentRoot[] roots= fSourceFolder.getJavaProject().getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].getElementName().equals(getNewElementName()))
				return roots[i];	
		}
		return null;
	}
	
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fSourceFolder, new RenameArguments(getNewElementName(), getUpdateReferences()));
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		return new IFile[0];
	}
	
	//---- IRenameProcessor ----------------------------------------------
	
	public String getCurrentElementName() {
		return fSourceFolder.getElementName();
	}
			
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}

	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		if (! newName.trim().equals(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_blank); 
		
		IContainer c= 	fSourceFolder.getResource().getParent();
		if (! c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_invalid_name); 
		
		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, IResource.FOLDER));
		if (result.hasFatalError())
			return result;		
				
		result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), IResource.FOLDER)));		
		if (result.hasFatalError())
			return result;
			
		IJavaProject project= fSourceFolder.getJavaProject();
		IPath p= project.getProject().getFullPath().append(newName);
		if (project.findPackageFragmentRoot(p) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_already_exists); 
		
		if (project.getProject().findMember(new Path(newName)) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameSourceFolderRefactoring_alread_exists); 
		return result;		
	}
	
	private String createNewPath(String newName) {
		return fSourceFolder.getPath().removeLastSegments(1).append(newName).toString();
	}

	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}		
	}
	
	public boolean getUpdateReferences() {
		return true;
	}
	
	//-- changes

	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new DynamicValidationStateChange(
				new RenameSourceFolderChange(fSourceFolder, getNewElementName(), getComment()));
		} finally{
			pm.done();
		}	
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments generic= (JavaRefactoringArguments) arguments;
			final String path= generic.getAttribute(ATTRIBUTE_PATH);
			if (path != null) {
				final IResource resource= ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(path));
				if (resource == null || !resource.exists())
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, RenameSourceFolderChange.ID_RENAME_SOURCE_FOLDER));
				else
					fSourceFolder= (IPackageFragmentRoot) JavaCore.create(resource);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PATH));
			final String name= generic.getAttribute(ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_NAME));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}