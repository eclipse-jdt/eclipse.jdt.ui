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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.RefactoringStyles;
import org.eclipse.jdt.internal.corext.refactoring.participants.RenameProcessor;

public class RenameResourceProcessor extends RenameProcessor {

	private IResource fResource;

	//---- IRenameProcessor methods ---------------------------------------
		
	public RenameResourceProcessor() {
		super(RefactoringStyles.NONE);
	}

	public void initialize(Object[] elements) throws CoreException {
		Assert.isTrue(elements != null && elements.length == 1);
		Object element= elements[0];
		if (!(element instanceof IAdaptable))
			return;
		fResource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
		if (fResource == null)
			return;
		setNewElementName(fResource.getName());
	}
	
	public boolean isAvailable() throws JavaModelException {
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
				new String[]{getCurrentElementName(), fNewElementName});
		return message;
	}
	
	public Object[] getElements() {
		return new Object[] {fResource};
	}
	
	public String getCurrentElementName() {
		return fResource.getName();
	}
	
	public IResourceModifications getResourceModifications() throws CoreException {
		return null;	
	}
		
	public IProject[] getAffectedProjects() {
		return ResourceProcessors.computeScope(fResource);
	}

	public Object getNewElement() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(createNewPath(fNewElementName));
	}

	//--- Condition checking --------------------------------------------

	public RefactoringStatus checkActivation() throws CoreException {
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
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
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
			  new RenameResourceChange(fResource, fNewElementName));
		} finally{
			pm.done();
		}	
	}
}

