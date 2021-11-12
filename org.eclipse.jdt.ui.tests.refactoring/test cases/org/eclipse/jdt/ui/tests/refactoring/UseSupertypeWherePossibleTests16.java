/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified from UseSupertypeWherePossibleTests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.descriptors.UseSupertypeDescriptor;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

public class UseSupertypeWherePossibleTests16 extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "UseSupertypeWherePossible16/";

	public UseSupertypeWherePossibleTests16() {
		rts= new Java16Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Before
	public void before() throws Exception {
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID,
				"${package_declaration}" +
					System.getProperty("line.separator", "\n") +
				"${"+ CodeTemplateContextType.TYPE_COMMENT+"}" +
				System.getProperty("line.separator", "\n") +
				"${type_declaration}", null);

			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/** typecomment template*/", null);
	}

	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void validatePassingTest(String className, String[] cuNames, String superTypeFullName, boolean replaceInstanceOf) throws Exception {
		final IType subType= getClassFromTestFile(getPackageP(), className);
		final ICompilationUnit[] units= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			if (cuNames[i].equals(subType.getCompilationUnit().findPrimaryType().getElementName()))
				units[i]= subType.getCompilationUnit();
			else
				units[i]= createCUfromTestFile(subType.getPackageFragment(), cuNames[i]);

		}
		final IType superType= subType.getJavaProject().findType(superTypeFullName, (IProgressMonitor) null);
		final UseSupertypeDescriptor descriptor= RefactoringSignatureDescriptorFactory.createUseSupertypeDescriptor();
		descriptor.setSubtype(subType);
		descriptor.setSupertype(superType);
		descriptor.setReplaceInstanceof(replaceInstanceOf);
		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue("status should be ok", status.isOK());
		assertNotNull("refactoring should not be null", refactoring);
		assertNull("was supposed to pass", performRefactoring(refactoring));

		for (int i= 0; i < units.length; i++) {
			String expected= getFileContents(getOutputTestFileName(cuNames[i]));
			String actual= units[i].getSource();
			String message= "incorrect changes in " + units[i].getElementName();
			assertEqualLines(message, expected, actual);
		}
	}

	private void validatePassingTest(String className, String[] cuNames, String superTypeFullName) throws Exception {
		validatePassingTest(className, cuNames, superTypeFullName, false);
	}

	//---------------tests ----------------------

	@Test
	public void test0_() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void test1_() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "p.B", true);
	}

}