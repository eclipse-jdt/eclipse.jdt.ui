/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d5Setup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Test suite for the extract supertype tests.
 *
 * Note: Extract Supertype heavily relies on PullUpRefactoring and its tests
 */
public final class ExtractSupertypeTests extends GenericRefactoringTest {

	public ExtractSupertypeTests() {
		rts= new Java1d5Setup();
	}

	private static final String REFACTORING_PATH= "ExtractSupertype/";

	private static ExtractSupertypeProcessor createRefactoringProcessor(IMember[] members) throws JavaModelException {
		IJavaProject project= null;
		if (members != null && members.length > 0)
			project= members[0].getJavaProject();
		if (RefactoringAvailabilityTester.isExtractSupertypeAvailable(members)) {
			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(project);
			settings.createComments= false;
			ExtractSupertypeProcessor processor= new ExtractSupertypeProcessor(members, settings);
			new ProcessorBasedRefactoring(processor);
			return processor;
		}
		return null;
	}

	private static IMethod[] getMethods(IMember[] members) {
		List<IJavaElement> l= Arrays.asList(JavaElementUtil.getElementsOfType(members, IJavaElement.METHOD));
		return l.toArray(new IMethod[l.size()]);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1(String[] methodNames, String[][] signatures, String[] fieldNames, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods, boolean replaceOccurences) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType type= getType(cu, "B");
		IMethod[] methods= getMethods(type, methodNames, signatures);

		IField[] fields= getFields(type, fieldNames);

		IMember[] members= merge(methods, fields);
		ExtractSupertypeProcessor processor= createRefactoringProcessor(members);
		Refactoring refactoring= processor.getRefactoring();

		assertTrue("activation", refactoring.checkInitialConditions(new NullProgressMonitor()).isOK());

		processor.setMembersToMove(members);
		processor.setTypesToExtract(new IType[] { type});
		processor.setTypeName("Z");
		processor.setCreateMethodStubs(false);
		processor.setInstanceOf(false);
		processor.setReplace(replaceOccurences);
		if (deleteAllInSourceType)
			processor.setDeletedMethods(getMethods(members));
		if (deleteAllMatchingMethods)
			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

		RefactoringStatus status= refactoring.checkFinalConditions(new NullProgressMonitor());
		assertFalse("precondition was supposed to pass", status.hasError());
		performChange(refactoring, false);

		String expected= getFileContents(getOutputTestFileName("A"));
		ICompilationUnit unit= getPackageP().getCompilationUnit("A.java");
		String actual= unit.getBuffer().getContents();
		assertEqualLines(expected, actual);

		expected= getFileContents(getOutputTestFileName("Z"));
		unit= getPackageP().getCompilationUnit("Z.java");
		assertTrue("extracted compilation unit does not exist", unit.exists());
		actual= unit.getBuffer().getContents();
		assertEqualLines(expected, actual);
	}

	@Test
	public void test0() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, null, true, false, true);
	}

	@Test
	public void test1() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, null, true, false, true);
	}

	@Test
	public void test2() throws Exception {
		helper1(new String[] { "m", "n"}, new String[][] { new String[0], new String[0]}, null, true, false, true);
	}

	@Test
	public void test3() throws Exception {
		helper1(new String[] { "m", "n"}, new String[][] { new String[0], new String[0]}, null, true, false, true);
	}

	@Test
	public void test4() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, null, true, false, true);
	}

	@Test
	public void testBug151683() throws Exception {
		helper1(new String[] { "m" }, new String[][] { new String[0] }, null, true, false, false);
	}

	@Test
	public void testBug240353() throws Exception {
		helper1(new String[] { "foo" }, new String[][] { new String[] { Signature.createTypeSignature("T", false) } },
				null, true, false, false);
	}

	@Test
	public void testBug573884_1() throws Exception {
		helper1(new String[] { "geta", "getb" }, new String[][] { new String[0], new String[0] }, new String[] { "a", "b" }, true, false, true);
	}

	@Test
	public void testBug573884_2() throws Exception {
		helper1(new String[] { "geta", "getb" }, new String[][] { new String[0], new String[0] }, new String[] { "a", "b" }, true, false, true);
	}

	@Test
	public void testBug573884_3() throws Exception {
		helper1(new String[] { "geta", "getb" }, new String[][] { new String[0], new String[0] }, new String[] { "a", "b" }, true, false, true);
	}
}
