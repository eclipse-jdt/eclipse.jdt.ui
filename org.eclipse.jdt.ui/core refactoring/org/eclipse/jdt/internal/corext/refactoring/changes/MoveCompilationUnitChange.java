/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class MoveCompilationUnitChange extends CompilationUnitReorgChange {

	private boolean fUndoable;
	
	public MoveCompilationUnitChange(ICompilationUnit cu, IPackageFragment newPackage){
		super(cu, newPackage);
	}
	
	private MoveCompilationUnitChange(IPackageFragment oldPackage, String cuName, IPackageFragment newPackage){
		super(oldPackage.getHandleIdentifier(), newPackage.getHandleIdentifier(), oldPackage.getCompilationUnit(cuName).getHandleIdentifier());
	}
	
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("MoveCompilationUnitChange.name", //$NON-NLS-1$
		new String[]{getCu().getElementName(), getPackageName(getDestinationPackage())}); 
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, false, false);
	}
	
	Change doPerformReorg(IProgressMonitor pm) throws JavaModelException{
		String name;
		String newName= getNewName();
		if (newName == null)
			name= getCu().getElementName();
		else
			name= newName;	
		fUndoable= ! getDestinationPackage().getCompilationUnit(name).exists();
		
		getCu().move(getDestinationPackage(), null, newName, true, pm);
		if (fUndoable) {
			return new MoveCompilationUnitChange(getDestinationPackage(), getCu().getElementName(), getOldPackage());
		} else {
			return null;
		}
	}
}
