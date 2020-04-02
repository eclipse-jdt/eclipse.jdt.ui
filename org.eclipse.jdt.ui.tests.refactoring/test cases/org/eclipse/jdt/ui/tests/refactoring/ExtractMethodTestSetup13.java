/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.ui.tests.refactoring.rules.Java13Setup;

public class ExtractMethodTestSetup13 extends Java13Setup {

	private IPackageFragment fTry13Package;
	private IPackageFragment fInvalidSelectionPackage;

	@Override
	public void before() throws Exception {
		super.before();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fTry13Package= root.createPackageFragment("try13_in", true, null);
		fInvalidSelectionPackage= root.createPackageFragment("invalidSelection13", true, null);
	}

	public IPackageFragment getTry13Package() {
		return fTry13Package;
	}

	public IPackageFragment getInvalidSelectionPackage() {
		return fInvalidSelectionPackage;
	}
}
