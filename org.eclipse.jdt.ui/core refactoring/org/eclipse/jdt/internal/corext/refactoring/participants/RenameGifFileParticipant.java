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

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;


public class RenameGifFileParticipant extends RenameParticipant {
	
	private IFile fFile;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#init(java.lang.Object)
	 */
	public void initialize(RenameRefactoring refactoring, Object elementToBeRenamed) {
		super.initialize(refactoring);
		Assert.isTrue(elementToBeRenamed instanceof IFile);
		fFile= (IFile)elementToBeRenamed;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#canParticipate()
	 */
	public boolean isAvailable() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#getElement()
	 */
	public Object getElement() {
		// TODO Auto-generated method stub
		return fFile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#checkActivation()
	 */
	public RefactoringStatus checkActivation() {
		RefactoringStatus result= new RefactoringStatus();
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#checkInput(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.IRenameParticipant#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws CoreException {
		System.out.println("Update references in HTML files to Gif file " + fFile.getName());
		return new NullChange();
	}
}
