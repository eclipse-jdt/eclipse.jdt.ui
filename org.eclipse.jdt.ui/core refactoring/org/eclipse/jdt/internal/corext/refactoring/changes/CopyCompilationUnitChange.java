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
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CopyCompilationUnitChange extends CompilationUnitReorgChange {
	
	public CopyCompilationUnitChange(ICompilationUnit cu, IPackageFragment dest, INewNameQuery newNameQuery){
		super(cu, dest, newNameQuery);
	}
		
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, false, false);
	}
	
	Change doPerformReorg(IProgressMonitor pm) throws JavaModelException{
		getCu().copy(getDestinationPackage(), null, getNewName(), true, pm);
		return null;
	}

	public String getName() {
		return RefactoringCoreMessages.getFormattedString("CopyCompilationUnitChange.copy", //$NON-NLS-1$
			new String[]{getCu().getElementName(), getPackageName(getDestinationPackage())});
	}

}

