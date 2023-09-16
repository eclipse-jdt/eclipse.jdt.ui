/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

public class MovePackageFragmentRootChange extends PackageFragmentRootReorgChange {

	public MovePackageFragmentRootChange(IPackageFragmentRoot root, IContainer destination, IPackageFragmentRootManipulationQuery updateClasspathQuery) {
		super(root, destination, null, updateClasspathQuery);
	}

	@Override
	protected Change doPerformReorg(IPath destinationPath, IProgressMonitor pm) throws JavaModelException {
		getRoot().move(destinationPath, getResourceUpdateFlags(), getUpdateModelFlags(false), null, pm);
		return null;
	}

	@Override
	public String getName() {
		String rootName= JavaElementLabelsCore.getElementLabel(getRoot(), JavaElementLabelsCore.ALL_DEFAULT);
		String destinationName= BasicElementLabels.getResourceName(getDestination());
		return Messages.format(RefactoringCoreMessages.MovePackageFragmentRootChange_move, new String[] { rootName, destinationName });
	}
}
