/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

public class ExtractMethodTestSetup18 extends Java18Setup {

	private IPackageFragment fDefaultMethodsPackage;
	private IPackageFragment fStaticMethodsPackage;
	private IPackageFragment fDestinationPackage;
	private IPackageFragment fLambdaExpressionPackage;

	public ExtractMethodTestSetup18(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

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
