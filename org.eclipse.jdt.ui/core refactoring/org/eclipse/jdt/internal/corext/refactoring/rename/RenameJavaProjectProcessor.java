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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameJavaProjectChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;


public class RenameJavaProjectProcessor extends JavaRenameProcessor implements IReferenceUpdating {

	private IJavaProject fProject;
	private boolean fUpdateReferences;
	
	private static final String IDENTIFIER= "org.eclipse.jdt.ui.renameJavaProjectProcessor"; //$NON-NLS-1$
	
	//---- IRefactoringProcessor ---------------------------------------------------
	
	public RenameJavaProjectProcessor(IJavaProject project) {
		initialize(project);
	}

	public void initialize(Object[] elements) {
		Assert.isTrue(elements != null && elements.length == 1);
		Object element= elements[0];
		if (!(element instanceof IJavaProject))
			return;
		initialize((IJavaProject)element);
	}
	
	private void initialize(IJavaProject project) {
		fProject= project;
		setNewElementName(fProject.getElementName());
		fUpdateReferences= true;
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		if (fProject == null)
			return false;
		if (! Checks.isAvailable(fProject))	
			return false;
		if (! fProject.isConsistent())
			return false;	
		return true;
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString(
			"RenameJavaProjectRefactoring.rename", //$NON-NLS-1$
			new String[]{getCurrentElementName(), getNewElementName()});
	}
	
	protected IProject[] getAffectedProjects() throws CoreException {
		return JavaProcessors.computeScope(fProject);
	}
	
	public Object[] getElements() {
		return new Object[] {fProject};
	}

	public RefactoringParticipant[] getSecondaryParticipants() throws CoreException {
		return createSecondaryParticipants(null, null, computeResourceModifications());
	}
	
	private ResourceModifications computeResourceModifications() throws CoreException {
		ResourceModifications result= new ResourceModifications();
		result.setRename(fProject.getProject(), getArguments());
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
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= RefactoringStatus.create(ResourcesPlugin.getWorkspace().validateName(newName, IResource.PROJECT));
		if (result.hasFatalError())
			return result;
		
		if (projectNameAlreadyExists(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameJavaProjectRefactoring.already_exists")); //$NON-NLS-1$
		
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			if (isReadOnly()){
				String message= RefactoringCoreMessages.getFormattedString("RenameJavaProjectRefactoring.read_only", //$NON-NLS-1$
									fProject.getElementName());
				return RefactoringStatus.createErrorStatus(message);
			}	
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	private boolean isReadOnly() throws CoreException {
		return fProject.getResource().isReadOnly();
	}
	
	private boolean projectNameAlreadyExists(String newName){
		return fProject.getJavaModel().getJavaProject(newName).exists();
	}

	//--- changes 
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new ValidationStateChange(
				new RenameJavaProjectChange(fProject, getNewElementName(), fUpdateReferences));
		} finally{
			pm.done();
		}	
	}
}