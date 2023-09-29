/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.util.Messages;

public class CopyPackageChange extends PackageReorgChange {

	public CopyPackageChange(IPackageFragment pack, IPackageFragmentRoot dest, INewNameQuery nameQuery){
		super(pack, dest, nameQuery);
	}

	@Override
	protected Change doPerformReorg(IProgressMonitor pm) throws JavaModelException, OperationCanceledException {
		getPackage().copy(getDestination(), null, getNewName(), true, pm);
		return null;
	}

	@Override
	public String getName() {
		String packageName= JavaElementLabelsCore.getElementLabel(getPackage(), JavaElementLabelsCore.ALL_DEFAULT);
		String destinationName= JavaElementLabelsCore.getElementLabel(getDestination(), JavaElementLabelsCore.ALL_DEFAULT);
		return Messages.format(RefactoringCoreMessages.CopyPackageChange_copy, new String[]{ packageName, destinationName});
	}
}
