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
 *     Red Hat Inc. - created based on MoveMembersTests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;

import org.eclipse.jdt.ui.tests.refactoring.ccp.MockReorgQueries;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class MoveInitializerTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "MoveInitializer/";

	public MoveInitializerTests() {
		rts= new RefactoringTestSetup();
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

	// Move static initializer in same package and ensure imports are brought along
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=108147
	@SuppressWarnings("null")
	@Test
	public void test0() throws Exception{
		ParticipantTesting.reset();
		final String p1Name= "p1";
		final String inDir= "/in/";
		final String outDir= "/out/";

		IPackageFragment packP1= createPackage(p1Name);
		ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
		ICompilationUnit p1B= createCu(packP1, getName() + inDir + p1Name + "/B.java", "B.java");

		IResource[] resources= {};
		IType bType= p1B.getTypes()[0];
		IInitializer initializerB= bType.getInitializer(1);
		IJavaElement[] javaElements= {initializerB};
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
		JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
		processor.setReorgQueries(new MockReorgQueries());
		processor.setDestination(ReorgDestinationFactory.createDestination(p1A));
		processor.setUpdateReferences(true);
		performDummySearch();
		RefactoringStatus status= performRefactoring(processor, true);

		//-- checks
		assertNull("status should be ok here", status);

		assertEquals("p1 files", 2, packP1.getChildren().length);

		String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/A.java");
		assertEqualLines("incorrect update of A", expectedSource, packP1.getCompilationUnit("A.java").getSource());

		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/B.java");
		assertEqualLines("incorrect update of B", expectedSource, packP1.getCompilationUnit("B.java").getSource());
	}

	// Move static initializer in different package and ensure imports are brought along
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=108147
	@SuppressWarnings("null")
	@Test
	public void test1() throws Exception{
		ParticipantTesting.reset();
		final String p1Name= "p1";
		final String p2Name= "p2";
		final String inDir= "/in/";
		final String outDir= "/out/";

		IPackageFragment packP1= createPackage(p1Name);
		ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
		IPackageFragment packP2= createPackage(p2Name);
		ICompilationUnit p1B= createCu(packP2, getName() + inDir + p2Name + "/B.java", "B.java");

		IResource[] resources= {};
		IType bType= p1B.getTypes()[0];
		IInitializer initializerB= bType.getInitializer(1);
		IJavaElement[] javaElements= {initializerB};
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
		JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
		processor.setReorgQueries(new MockReorgQueries());
		processor.setDestination(ReorgDestinationFactory.createDestination(p1A));
		processor.setUpdateReferences(true);
		performDummySearch();
		RefactoringStatus status= performRefactoring(processor, true);

		//-- checks
		assertNull("status should be ok here", status);

		assertEquals("p1 files", 1, packP1.getChildren().length);
		assertEquals("p2 files", 1, packP2.getChildren().length);

		String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/A.java");
		assertEqualLines("incorrect update of A", expectedSource, packP1.getCompilationUnit("A.java").getSource());

		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/B.java");
		assertEqualLines("incorrect update of B", expectedSource, packP2.getCompilationUnit("B.java").getSource());
	}

	private RefactoringStatus performRefactoring(JavaMoveProcessor processor, boolean providesUndo) throws Exception {
		return performRefactoring(new MoveRefactoring(processor), providesUndo);
	}
}
