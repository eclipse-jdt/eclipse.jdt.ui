/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class RewriteMethodInvocationsTestSetup extends RefactoringTestSetup {

	private IPackageFragment fRewrite;

	public RewriteMethodInvocationsTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fRewrite= root.createPackageFragment("rewrite_in", true, null);
	}

	public IPackageFragment getRewritePackage() {
		return fRewrite;
	}
}
