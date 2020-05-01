/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class SefTestSetup extends RefactoringTestSetup {

	private IPackageFragment fBaseTypes;
	private IPackageFragment fObjectTypes;
	private IPackageFragment fInvalid;
	private IPackageFragment fStatic;
	private IPackageFragment fStaticRef;
	private IPackageFragment fExistingMethod;


	@Override
	public void before() throws Exception {
		super.before();

		IPackageFragmentRoot root= getDefaultSourceFolder();

		fBaseTypes= root.createPackageFragment("base_in", true, null);
		fObjectTypes= root.createPackageFragment("object_in", true, null);
		fInvalid= root.createPackageFragment("invalid", true, null);
		fStatic= root.createPackageFragment("static_in", true, null);
		fStaticRef= root.createPackageFragment("static_ref_in", true, null);
		fExistingMethod= root.createPackageFragment("existingmethods_in", true, null);
	}

	public IPackageFragment getBasePackage() {
		return fBaseTypes;
	}

	public IPackageFragment getObjectPackage() {
		return fObjectTypes;
	}

	public IPackageFragment getInvalidPackage() {
		return fInvalid;
	}

	public IPackageFragment getStaticPackage() {
		return fStatic;
	}

	public IPackageFragment getStaticRefPackage() {
		return fStaticRef;
	}

	public IPackageFragment getExistingMethodPackage(){
		return fExistingMethod;
	}
}
