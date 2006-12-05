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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public class MoveCompilationUnitChange extends CompilationUnitReorgChange {

	private boolean fUndoable;
	private long fStampToRestore;
	
	public MoveCompilationUnitChange(ICompilationUnit cu, IPackageFragment newPackage){
		super(cu, newPackage);
		fStampToRestore= IResource.NULL_STAMP;
	}
	
	private MoveCompilationUnitChange(IPackageFragment oldPackage, String cuName, IPackageFragment newPackage, long stampToRestore) {
		super(oldPackage.getHandleIdentifier(), newPackage.getHandleIdentifier(), oldPackage.getCompilationUnit(cuName).getHandleIdentifier());
		fStampToRestore= stampToRestore;
	}
	
	public String getName() {
		return Messages.format(RefactoringCoreMessages.MoveCompilationUnitChange_name, 
		new String[]{getCu().getElementName(), getPackageName(getDestinationPackage())}); 
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, READ_ONLY | SAVE_IF_DIRTY);
	}
	
	Change doPerformReorg(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		String name;
		String newName= getNewName();
		if (newName == null)
			name= getCu().getElementName();
		else
			name= newName;
		
		// get current modification stamp
		long currentStamp= IResource.NULL_STAMP;
		IResource resource= getCu().getResource();
		if (resource != null) {
			currentStamp= resource.getModificationStamp();
		}
		
		fUndoable= ! getDestinationPackage().getCompilationUnit(name).exists();
		
		// perform the move and restore modification stamp
		getCu().move(getDestinationPackage(), null, newName, true, pm);
		if (fStampToRestore != IResource.NULL_STAMP) {
			ICompilationUnit moved= getDestinationPackage().getCompilationUnit(name);
			IResource movedResource= moved.getResource();
			if (movedResource != null) {
				movedResource.revertModificationStamp(fStampToRestore);
			}
		}
		
		if (fUndoable) {
			return new MoveCompilationUnitChange(getDestinationPackage(), getCu().getElementName(), getOldPackage(), currentStamp);
		} else {
			return null;
		}
	}
}
