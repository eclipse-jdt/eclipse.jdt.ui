/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * This class is made to run tests on Java Spider 1.8 .
 */
public class ExtractMethodTestSetup1d8 extends Java1d8Setup {
	private IPackageFragment fDefaultMethodsPackage;
	private IPackageFragment fStaticMethodsPackage;
	private IPackageFragment fDestinationPackage;
	private IPackageFragment fLambdaExpressionPackage;

	@Override
	public void before() throws Exception {
		super.before();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fDefaultMethodsPackage= root.createPackageFragment("defaultMethods18_in", true, null);
		fStaticMethodsPackage= root.createPackageFragment("staticMethods18_in", true, null);
		fDestinationPackage= root.createPackageFragment("destination18_in", true, null);
		fLambdaExpressionPackage= root.createPackageFragment("lambdaExpression18_in", true, null);
	}

	public IPackageFragment getDefaultMethodsPackage() {
		return fDefaultMethodsPackage;
	}

	public IPackageFragment getStaticMethodsPackage() {
		return fStaticMethodsPackage;
	}

	public IPackageFragment getDestinationPackage() {
		return fDestinationPackage;
	}

	public IPackageFragment getLambdaExpressionPackage() {
		return fLambdaExpressionPackage;
	}
}
