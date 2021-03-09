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

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

public class InlineMethodTestSetup16 extends Java16Setup {

	private IPackageFragment fSimple14;

	private IPackageFragment fDefault;

	private String simplePkgInName= "simple14_in";

	private String simplePkgOutName= "simple14_out";

	@Override
	public void before() throws Exception {
		super.before();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fSimple14= root.createPackageFragment(simplePkgInName, true, null);
		fDefault= root.getPackageFragment("");
	}

	public IPackageFragment getSimplePackage() {
		return fSimple14;
	}

	public IPackageFragment getDefaultPackage() {
		return fDefault;
	}

	public String getSimplePkgOutName() {
		return simplePkgOutName;
	}
}
