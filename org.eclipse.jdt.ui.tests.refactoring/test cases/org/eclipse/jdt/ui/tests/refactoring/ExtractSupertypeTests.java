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

import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Test suite for the extract supertype tests.
 *
 * Note: Extract Supertype heavily relies on PullUpRefactoring and its tests
 */
public final class ExtractSupertypeTests extends RefactoringTest {

	private static final Class clazz= ExtractSupertypeTests.class;

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
		List l= Arrays.asList(JavaElementUtil.getElementsOfType(members, IJavaElement.METHOD));
		return (IMethod[]) l.toArray(new IMethod[l.size()]);
	}

	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	public ExtractSupertypeTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods, boolean replaceOccurences) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try {
			IType type= getType(cu, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			ExtractSupertypeProcessor processor= createRefactoringProcessor(methods);
			Refactoring refactoring= processor.getRefactoring();
			processor.setMembersToMove(methods);

			assertTrue("activation", refactoring.checkInitialConditions(new NullProgressMonitor()).isOK());

			processor.setTypesToExtract(new IType[] { type});
			processor.setTypeName("Z");
			processor.setCreateMethodStubs(true);
			processor.setInstanceOf(false);
			processor.setReplace(replaceOccurences);
			if (deleteAllInSourceType)
				processor.setDeletedMethods(methods);
			if (deleteAllMatchingMethods)
				processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus status= refactoring.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to pass", !status.hasError());
			performChange(refactoring, false);

			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEqualLines(expected, actual);

			expected= getFileContents(getOutputTestFileName("Z"));
			ICompilationUnit unit= getPackageP().getCompilationUnit("Z.java");
			if (!unit.exists())
				assertTrue("extracted compilation unit does not exist", false);
			actual= unit.getBuffer().getContents();
			assertEqualLines(expected, actual);

		} finally {
			performDummySearch();
			cu.delete(false, null);
		}
	}

	public void test0() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, true);
	}

	public void test1() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, true);
	}

	public void test2() throws Exception {
		helper1(new String[] { "m", "n"}, new String[][] { new String[0], new String[0]}, true, false, true);
	}

	public void test3() throws Exception {
		helper1(new String[] { "m", "n"}, new String[][] { new String[0], new String[0]}, true, false, true);
	}

	public void test4() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, true);
	}

	public void testBug151683() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, false);
	}
}
