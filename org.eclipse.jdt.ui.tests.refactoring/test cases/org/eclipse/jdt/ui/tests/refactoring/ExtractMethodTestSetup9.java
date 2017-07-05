/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import junit.framework.Test;

public class ExtractMethodTestSetup9 extends Java9Setup {

	private IPackageFragment fTry9Package;

	private IPackageFragment fInvalidSelectionPackage;

	public ExtractMethodTestSetup9(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

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
