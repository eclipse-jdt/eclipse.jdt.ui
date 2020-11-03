/**
 * Copyright (c) 2020s IBM Corporation and others.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ChangeSignatureTests15 extends ChangeSignatureTests {
	@Rule
	public Java15ProjectTestSetup jps= new Java15ProjectTestSetup(true);

	private IJavaProject fJProject1;

	@Override
	protected String getRefactoringPath() {
		return "ChangeSignature15/";
	}

	@Override
	public void genericafter() throws Exception {
		JavaProjectHelper.clear(fJProject1, jps.getDefaultClasspath());
	}

	@Override
	public void genericbefore() throws Exception {
		fJProject1= jps.getProject();
		fRoot= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackageP= fRoot.createPackageFragment("p", true, null);
		fIsPreDeltaTest= false;
		RefactoringCore.getUndoManager().flush();
	}

	// Exchange the method parameters
	@Test
	public void testRecordMethod() throws Exception {
		String[] signature= {};
		helperRenameMethod(signature, "newName", false, false);
	}

	// Rename method involving method reference
	@Test
	public void testRecordMethodReference() throws Exception {
		String[] signature= {};
		helperRenameMethod(signature, "newName", false, true);
	}
}