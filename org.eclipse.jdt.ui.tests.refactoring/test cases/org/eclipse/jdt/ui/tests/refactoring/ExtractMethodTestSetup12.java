/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import junit.framework.Test;

public class ExtractMethodTestSetup12 extends Java12Setup {

	private IPackageFragment fTry12Package;
	private IPackageFragment fInvalidSelectionPackage;

	public ExtractMethodTestSetup12(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fTry12Package= root.createPackageFragment("try12_in", true, null);
		fInvalidSelectionPackage= root.createPackageFragment("invalidSelection12", true, null);
	}

	public IPackageFragment getTry12Package() {
		return fTry12Package;
	}

	public IPackageFragment getInvalidSelectionPackage() {
		return fInvalidSelectionPackage;
	}
}
