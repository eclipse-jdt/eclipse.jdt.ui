/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * This class is made to run tests on Java Spider 1.8 .
 */
public class InlineMethodTestSetup1d8 extends Java1d8Setup {
	private IPackageFragment fSimple18;

	@Override
	public void before() throws Exception {
		super.before();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fSimple18= root.createPackageFragment("simple18_in", true, null);
	}

	public IPackageFragment getSimplePackage() {
		return fSimple18;
	}
}
