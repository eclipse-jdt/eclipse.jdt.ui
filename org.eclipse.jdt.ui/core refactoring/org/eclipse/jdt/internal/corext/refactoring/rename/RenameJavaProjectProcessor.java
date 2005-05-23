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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameJavaProjectChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;


public class RenameJavaProjectProcessor extends JavaRenameProcessor implements IReferenceUpdating {

	private IJavaProject fProject;
	private boolean fUpdateReferences;
	
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameJavaProjectProcessor"; //$NON-NLS-1$
	
	//---- IRefactoringProcessor ---------------------------------------------------
	
	public RenameJavaProjectProcessor(IJavaProject project) {
		fProject= project;
		setNewElementName(fProject.getElementName());
		fUpdateReferences= true;
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fProject);
	}
	
	public String getProcessorName() {
		return Messages.format(
			RefactoringCoreMessages.RenameJavaProjectRefactoring_rename, //$NON-NLS-1$
			new String[]{getCurrentElementName(), getNewElementName()});
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fProject);
	}
	
	public Object[] getElements() {
		return new Object[] {fProject};
	}

	protected void loadDerivedParticipants(RefactoringStatus status, List result, String[] natures, SharableParticipants shared) throws CoreException {
		loadDerivedParticipants(status, result, 
			null, null, 
			computeResourceModifications(), natures, shared);
	}
	
	private ResourceModifications computeResourceModifications() {
		ResourceModifications result= new ResourceModifications();
		result.setRename(fProject.getProject(), new RenameArguments(getNewElementName(), getUpdateReferences()));
		return result;		
	}
		 
	public Object getNewElement() throws CoreException {
		IPath newPath= fProject.getPath().removeLastSegments(1).append(getNewElementName());
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().findMember(newPath));
	}
	
	//---- IReferenceUpdating --------------------------------------
		
	public boolean canEnableUpdateReferences() {
		return true;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}

	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}
		
	//---- IRenameProcessor ----------------------------------------------
	
	public String getCurrentElementName() {
		return fProject.getElementName();
	}
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= RefactoringStatus.create(ResourcesPlugin.getWorkspace().validateName(newName, IResource.PROJECT));
		if (result.hasFatalError())
			return result;
		
		if (projectNameAlreadyExists(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameJavaProjectRefactoring_already_exists); 
		
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			if (isReadOnly()){
				String message= Messages.format(RefactoringCoreMessages.RenameJavaProjectRefactoring_read_only, 
									fProject.getElementName());
				return RefactoringStatus.createErrorStatus(message);
			}
			IFile projectFile= fProject.getProject().getFile(".project"); //$NON-NLS-1$
			if (projectFile != null && projectFile.exists()) {
				ValidateEditChecker validateEditChecker= (ValidateEditChecker) context.getChecker(ValidateEditChecker.class);
				validateEditChecker.addFile(projectFile);
			}
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	private boolean isReadOnly() {
		return Resources.isReadOnly(fProject.getResource());
	}
	
	private boolean projectNameAlreadyExists(String newName){
		return fProject.getJavaModel().getJavaProject(newName).exists();
	}

	//--- changes 
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new DynamicValidationStateChange(
				new RenameJavaProjectChange(fProject, getNewElementName(), fUpdateReferences));
		} finally{
			pm.done();
		}	
	}
}
