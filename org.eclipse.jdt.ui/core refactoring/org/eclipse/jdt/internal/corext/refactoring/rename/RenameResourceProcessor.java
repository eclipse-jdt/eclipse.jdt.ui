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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public class RenameResourceProcessor extends RenameProcessor implements INameUpdating {

	private IResource fResource;
	private String fNewElementName;
		
	private static final String IDENTIFIER= "org.eclipse.jdt.ui.renameResourceProcessor"; //$NON-NLS-1$
	
	public RenameResourceProcessor(IResource resource) {
		fResource= resource;
		if (fResource != null) {
			setNewElementName(fResource.getName());
		}
	}

	//---- INameUpdating ---------------------------------------------------
	
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewElementName= newName;
	}

	public String getNewElementName() {
		return fNewElementName;
	}
	
	//---- IRenameProcessor methods ---------------------------------------
		
	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws JavaModelException {
		if (fResource == null)
			return false;
		if (! fResource.exists())
			return false;
		if (! fResource.isAccessible())	
			return false;
		return true;			
	}
	
	public String getProcessorName() {
		String message= RefactoringCoreMessages.getFormattedString("RenameResourceProcessor.name", //$NON-NLS-1$
				new String[]{getCurrentElementName(), getNewElementName()});
		return message;
	}
	
	public Object[] getElements() {
		return new Object[] {fResource};
	}
	
	public String getCurrentElementName() {
		return fResource.getName();
	}
	
	public String[] getAffectedProjectNatures() throws CoreException {
		return ResourceProcessors.computeAffectedNatures(fResource);
	}

	public Object getNewElement() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(createNewPath(getNewElementName()));
	}

	public boolean getUpdateReferences() {
		return true;
	}
	
	public RefactoringParticipant[] loadParticipants(SharableParticipants shared) throws CoreException {
		Object[] elements= getElements();
		String[] natures= getAffectedProjectNatures();
		List result= new ArrayList();
		RenameArguments arguments= new RenameArguments(getNewElementName(), getUpdateReferences());
		for (int i= 0; i < elements.length; i++) {
			result.addAll(Arrays.asList(ParticipantManager.getRenameParticipants(this, 
				elements[i], arguments,
				natures, shared)));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	//--- Condition checking --------------------------------------------

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewElementName(String newName) throws JavaModelException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		IContainer c= fResource.getParent();
		if (c == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameResourceRefactoring.Internal_Error")); //$NON-NLS-1$
						
		if (c.findMember(newName) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameResourceRefactoring.alread_exists")); //$NON-NLS-1$
			
		if (!c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameResourceRefactoring.invalidName")); //$NON-NLS-1$
	
		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, fResource.getType()));
		if (! result.hasFatalError())
			result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), fResource.getType())));		
		return result;		
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	private String createNewPath(String newName){
		return fResource.getFullPath().removeLastSegments(1).append(newName).toString();
	}
		
	//--- changes 
	
	/* non java-doc 
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new ValidationStateChange(
			  new RenameResourceChange(fResource, getNewElementName()));
		} finally{
			pm.done();
		}	
	}
}

