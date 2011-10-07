/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class ExtractMethodTestSetup17 extends Java17Setup {

	private IPackageFragment fTry17Package;
	private IPackageFragment fInvalidSelectionPackage;

	public ExtractMethodTestSetup17(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

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

