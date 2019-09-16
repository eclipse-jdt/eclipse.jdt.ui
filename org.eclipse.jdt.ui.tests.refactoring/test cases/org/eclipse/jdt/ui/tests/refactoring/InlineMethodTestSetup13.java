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

import junit.framework.Test;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class InlineMethodTestSetup13 extends Java13Setup {

	private IPackageFragment fSimple13;

	public InlineMethodTestSetup13(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fSimple13= root.createPackageFragment("simple13_in", true, null);
	}

	public IPackageFragment getSimplePackage() {
		return fSimple13;
	}
}
