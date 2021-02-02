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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class CopyPackageChangeTest extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "CopyPackageChange/";

	public CopyPackageChangeTest() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Test
	public void test0() throws Exception{
		createCU(getPackageP(), "A.java", getFileContents(getRefactoringPath() + "A.java"));

		IPackageFragmentRoot newRoot= JavaProjectHelper.addSourceContainer(rts.getProject(), "newName");

		String packName= getPackageP().getElementName();
		CopyPackageChange change= new CopyPackageChange(getPackageP(), newRoot, null);
		change.initializeValidationData(new NullProgressMonitor());
		performChange(change);
		IPackageFragment copied= newRoot.getPackageFragment(packName);
		assertTrue("copied.exists()", copied.exists());
		assertEquals(1, copied.getChildren().length);
	}
}
