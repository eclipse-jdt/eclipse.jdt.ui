/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;

import org.eclipse.jdt.ui.tests.refactoring.ccp.MockReorgQueries;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class MoveCompilationUnitTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "MoveCu/";

	public MoveCompilationUnitTests() {
		this.rts= new RefactoringTestSetup();
	}

	protected MoveCompilationUnitTests(RefactoringTestSetup rts) {
		super(rts);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private String getQualifier(String qualifiedName) {
		int dot= qualifiedName.lastIndexOf('.');
		return qualifiedName.substring(0, dot != -1 ? dot : 0);
	}

	private String getSimpleName(String qualifiedName) {
		return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
	}

	private ICompilationUnit[] createCUs(String[] qualifiedNames) throws Exception {
		ICompilationUnit[] cus= new ICompilationUnit[qualifiedNames.length];
		for (int i= 0; i < qualifiedNames.length; i++) {
			Assert.isNotNull(qualifiedNames[i]);
			String qualifier= getQualifier(qualifiedNames[i]);
			String[] sections= qualifier.split("[.]");
			if (sections.length == 2) {
				cus[i]= createCUfromTestFile(getRoot().createPackageFragment(getQualifier(qualifiedNames[i]), true, null), getSimpleName(qualifiedNames[i]), sections[1] + "/");
			} else {
				cus[i]= createCUfromTestFile(getRoot().createPackageFragment(getQualifier(qualifiedNames[i]), true, null), getSimpleName(qualifiedNames[i]));
			}
		}
		return cus;
	}

	protected void doExecuteRefactoring(ICompilationUnit cunit, String destinationPackage) throws Exception {
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy((new IResource[0]), (new IJavaElement[] {cunit}));
		JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
		IPackageFragment destination= this.rts.getDefaultSourceFolder().createPackageFragment(destinationPackage, false, null);
		processor.setDestination(ReorgDestinationFactory.createDestination(destination));
		processor.setReorgQueries(new MockReorgQueries());
		processor.setUpdateReferences(true);
		MoveRefactoring ref= new MoveRefactoring(processor);
		RefactoringStatus status= performRefactoringWithStatus(ref);
		assertTrue(status.isOK());
	}

	@Test
	public void test_Issue1887() throws Exception {
		ICompilationUnit[] cus= createCUs(new String[] { "p1.MovingClass", "p3.TestMovingClass" });
		doExecuteRefactoring(cus[0], "p2");
		String output= cus[1].getSource();
		String outputTestFileName= getOutputTestFileName("TestMovingClass");
		String expectedOutput= getFileContents(outputTestFileName);
		assertEqualLines("move ", output, expectedOutput);
	}

}
