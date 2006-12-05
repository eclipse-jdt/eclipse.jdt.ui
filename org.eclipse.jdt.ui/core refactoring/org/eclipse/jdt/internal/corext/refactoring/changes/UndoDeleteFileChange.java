/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.undo.ResourceDescription;
import org.eclipse.jdt.internal.corext.util.Messages;


public class UndoDeleteFileChange extends Change {

	private final ResourceDescription fResourceDescription;

	public UndoDeleteFileChange(ResourceDescription resourceDescription) {
		fResourceDescription= resourceDescription;
	}
	
	public void initializeValidationData(IProgressMonitor pm) {
		
	}
	
	public Object getModifiedElement() {
		return null;
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.UndoDeleteFileChange_change_name, fResourceDescription.getName()); 
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (! fResourceDescription.isValid()) {
			return RefactoringStatus.createFatalErrorStatus(
					Messages.format(
							RefactoringCoreMessages.UndoDeleteFileChange_cannot_restore,
							fResourceDescription.getName()));
		}
		
		if (fResourceDescription.verifyExistence(true)) {
			return RefactoringStatus.createFatalErrorStatus(
					Messages.format(
							RefactoringCoreMessages.UndoDeleteFileChange_already_exists,
							fResourceDescription.getName()));
		}
		
		return new RefactoringStatus();
	}

	public Change perform(IProgressMonitor pm) throws CoreException {
		IFile file= (IFile) fResourceDescription.createResource(pm);
		return new DeleteFileChange(file, false);
	}
}
