/*******************************************************************************
 * Copyright (c) 2026 Advantest and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Advantest - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

public class RenameMethodWithSubtypeInLambdaTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "RenameMethodWithSubtypeInLambda/";

	public RenameMethodWithSubtypeInLambdaTests() {
		rts= new Java1d8Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Test
	public void testGH4704() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		IJavaProject project= rts.getProject();
		// Since we just created B.java, we need to close the project and re-open it to reproduce the bug.
		project.close();
		project.open(null);

		IType classA= getType(cuA, "A");
		IMethod method= classA.getMethod("m", null);

		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setJavaElement(method);
		descriptor.setNewName("k");
		descriptor.setUpdateReferences(true);
		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertNotNull("Refactoring should not be null", refactoring);
		assertTrue("status should be ok", status.isOK());

		assertNull("was supposed to pass", performRefactoring(refactoring));
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
		assertEqualLines("invalid renaming B", getFileContents(getOutputTestFileName("B")), cuB.getSource());
	}
}
