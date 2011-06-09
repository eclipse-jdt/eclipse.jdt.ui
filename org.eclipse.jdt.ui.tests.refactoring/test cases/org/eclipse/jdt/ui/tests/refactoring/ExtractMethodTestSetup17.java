/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import junit.framework.Test;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class ExtractMethodTestSetup17 extends Java17Setup {

	private IPackageFragment fTryPackage;

	public ExtractMethodTestSetup17(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fTryPackage= root.createPackageFragment("try17_in", true, null);
	}

	public IPackageFragment getTryPackage() {
		return fTryPackage;
	}
}

