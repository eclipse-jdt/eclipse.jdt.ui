/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;


public class RenameFileProcessor extends RenameProcessor {
	
	private IFile fFile;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#initialize(java.lang.Object)
	 */
	public void initialize(Object elementToBeRenamed) {
		Assert.isTrue(elementToBeRenamed instanceof IFile);
		fFile= (IFile)elementToBeRenamed;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor#getRefactoringName()
	 */
	public String getProcessorName() {
		return "Rename Resource"; //$NON-NLS-1$
	}

	public boolean isAvailable() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor#getScope()
	 */
	public IProject[] getScope() {
		return Processors.computeScope(fFile);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor#getElement()
	 */
	public Object getElement() {
		return fFile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor#getResourceModifications()
	 */
	public ResourceModifications getResourceModifications() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#checkActivation()
	 */
	public RefactoringStatus checkActivation() {
		RefactoringStatus result= new RefactoringStatus();
		if (fFile.isReadOnly())
			result.addFatalError("File is read only.");
		if (!fFile.isSynchronized(1))
			result.addFatalError("File is out of sync with underlying file system.");
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor#getCurrentName()
	 */
	public String getCurrentName() {
		return fFile.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#checkNewName(java.lang.String)
	 */
	public RefactoringStatus checkNewName(String newName) {
		RefactoringStatus result= new RefactoringStatus();
		IContainer container= fFile.getParent();
		if (container.findMember(newName) != null) {
			result.addFatalError("A resource with the name already exists");
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#checkInput(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws CoreException {
		return new RenameResourceChange(fFile, getNewName());
	}

}
