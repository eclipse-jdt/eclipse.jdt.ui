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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.ltk.core.refactoring.Change;

public class CopyPackageFragmentRootChange extends PackageFragmentRootReorgChange {

	public CopyPackageFragmentRootChange(IPackageFragmentRoot root, IProject destination, INewNameQuery newNameQuery, IPackageFragmentRootManipulationQuery updateClasspathQuery) {
		super(root, destination, newNameQuery, updateClasspathQuery);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.changes.PackageFragmentRootReorgChange#doPerform(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected Change doPerformReorg(IPath destinationPath, IProgressMonitor pm) throws JavaModelException {
		getRoot().copy(destinationPath, getResourceUpdateFlags(), getUpdateModelFlags(true), null, pm);
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getName()
	 */
	public String getName() {
		String[] keys= {getRoot().getElementName(), getDestinationProject().getName()};
		return RefactoringCoreMessages.getFormattedString("CopyPackageFragmentRootChange.copy", keys); //$NON-NLS-1$
	}

}
