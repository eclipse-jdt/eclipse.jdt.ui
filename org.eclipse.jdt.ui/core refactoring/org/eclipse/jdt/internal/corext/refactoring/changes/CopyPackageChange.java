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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.ltk.core.refactoring.Change;

public class CopyPackageChange extends PackageReorgChange {
	
	public CopyPackageChange(IPackageFragment pack, IPackageFragmentRoot dest, INewNameQuery nameQuery){
		super(pack, dest, nameQuery);
	}
	
	protected Change doPerformReorg(IProgressMonitor pm) throws JavaModelException{
		getPackage().copy(getDestination(), null, getNewName(), true, pm);
		return null;
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("CopyPackageChange.copy", //$NON-NLS-1$
			new String[]{ getPackage().getElementName(), getDestination().getElementName()});
	}
}

