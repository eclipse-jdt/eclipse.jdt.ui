/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.ui.tests.refactoring.rules.Java9Setup;

public class ExtractMethodTestSetup9 extends Java9Setup {

	private IPackageFragment fTry9Package;

	private IPackageFragment fInvalidSelectionPackage;

	@Override
	public void before() throws Exception {
		super.before();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fTry9Package= root.createPackageFragment("try9_in", true, null);
		fInvalidSelectionPackage= root.createPackageFragment("invalidSelection9", true, null);
	}

	public IPackageFragment getTry9Package() {
		return fTry9Package;
	}

	public IPackageFragment getInvalidSelectionPackage() {
		return fInvalidSelectionPackage;
	}
}
