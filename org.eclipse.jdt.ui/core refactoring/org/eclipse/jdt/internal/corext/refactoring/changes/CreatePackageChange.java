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

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CreatePackageChange extends JDTChange {
	
	private IPackageFragment fPackageFragment;
	
	public CreatePackageChange(IPackageFragment pack) {
		fPackageFragment= pack;
	}

	public RefactoringStatus isValid(IProgressMonitor pm) {
		return new RefactoringStatus();
	}
	
	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("CreatePackageChange.Creating_package"), 1); //$NON-NLS-1$

			if (fPackageFragment.exists()) {
				return new NullChange();	
			} else {
				IPackageFragmentRoot root= (IPackageFragmentRoot) fPackageFragment.getParent();
				root.createPackageFragment(fPackageFragment.getElementName(), false, pm);
				
				return new DeleteSourceManipulationChange(fPackageFragment);
			}		
		} finally {
			pm.done();
		}
	}

	public String getName() {
		return RefactoringCoreMessages.getString("CreatePackageChange.Create_package"); //$NON-NLS-1$
	}

	public Object getModifiedElement() {
		return fPackageFragment;
	}
}
