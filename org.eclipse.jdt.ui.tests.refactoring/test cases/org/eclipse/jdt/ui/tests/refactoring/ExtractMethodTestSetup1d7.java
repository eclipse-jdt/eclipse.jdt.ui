/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d7Setup;

/**
 * This class is used to run tests on Java Dolphin 1.7 .
 */
public class ExtractMethodTestSetup1d7 extends Java1d7Setup {

	private IPackageFragment fTry17Package;
	private IPackageFragment fInvalidSelectionPackage;

	@Override
	public void before() throws Exception {
		super.before();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fTry17Package= root.createPackageFragment("try17_in", true, null);
		fInvalidSelectionPackage= root.createPackageFragment("invalidSelection17", true, null);
	}

	public IPackageFragment getTry17Package() {
		return fTry17Package;
	}

	public IPackageFragment getInvalidSelectionPackage() {
		return fInvalidSelectionPackage;
	}
}

