/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
 *     Red Hat Inc. - created based on MoveMembersTests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveInnerToNewTests16 extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "MoveInnerToNew14/";

	public MoveInnerToNewTests16() {
		rts= new Java16Setup();
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		fIsPreDeltaTest= true;
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//---
	private IPackageFragment createPackage(String name) throws Exception{
		return getRoot().createPackageFragment(name, true, null);
	}

	private ICompilationUnit createCu(IPackageFragment pack, String cuPath, String cuName) throws Exception{
		return createCU(pack, cuName, getFileContents(getRefactoringPath() + cuPath));
	}

	@Test
	public void moveInnerRecord() throws Exception{
		ParticipantTesting.reset();
		final String p1Name= "p1";
		final String inDir= "/in/";
		final String outDir= "/out/";

		IPackageFragment packP1= createPackage(p1Name);
		ICompilationUnit p1Foo= createCu(packP1, getName() + inDir + p1Name + "/Foo.java", "Foo.java");
		IType fooType= p1Foo.getTypes()[0];
		IType barType= fooType.getTypes()[0];

		assertTrue("should be enabled", RefactoringAvailabilityTester.isMoveInnerAvailable(barType));
		MoveInnerToTopRefactoring ref= ((RefactoringAvailabilityTester.isMoveInnerAvailable(barType)) ? new MoveInnerToTopRefactoring(barType, JavaPreferencesSettings.getCodeGenerationSettings(barType.getJavaProject())) : null);
		assertNotNull("MoveInnerToTopRefactoring should not be null", ref);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful" + preconditionResult.toString(), preconditionResult.isOK());


		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertFalse("precondition was supposed to pass", checkInputResult.hasError());
		performChange(ref, false);

		assertEquals("p1 files", 2, packP1.getChildren().length);

		String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/Foo.java");
		assertEqualLines("incorrect update of Foo", expectedSource, packP1.getCompilationUnit("Foo.java").getSource());

		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/Bar.java");
		assertEqualLines("incorrect creation of Bar", expectedSource, packP1.getCompilationUnit("Bar.java").getSource());

	}

}
