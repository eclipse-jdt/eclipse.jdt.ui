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

import java.util.StringTokenizer;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceParameterRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class IntroduceParameterTests extends LineColumnSelectionTestCase {
	private static final String SLASH_OUT= "/out";

	@Rule
	public RefactoringTestSetup rts;

	public IntroduceParameterTests() {
		this.rts= new RefactoringTestSetup();
	}

	protected IntroduceParameterTests(RefactoringTestSetup rts) {
		this.rts= rts;
	}

	@Override
	protected String getResourceLocation() {
		return "IntroduceParameter/";
	}

	/**
	 * get names from comment in source "//name: guessedName -> nameToUse"
	 * <br>relies on tabwidth == 4
	 * @param cu
	 * @return {"guessedName", "nameToUse"} or null iff no name comment found
	 * @throws Exception
	 */
	private String[] getNames(ICompilationUnit cu) throws Exception {
		String source= cu.getSource();
		String name= "//name:";
		int namStart= source.indexOf(name);
		if (namStart == -1)
			return null;

		int dataStart= namStart + name.length();
		StringTokenizer tokenizer= new StringTokenizer(source.substring(dataStart), " ->\t\r\n");
		String[] result= {tokenizer.nextToken(), tokenizer.nextToken()};
		return result;
	}

	protected void performOK() throws Exception {
		perform(RefactoringStatus.OK, RefactoringStatus.OK);
	}

	protected void performInvalidSelection() throws Exception {
		perform(RefactoringStatus.FATAL, RefactoringStatus.FATAL);
	}

	protected void perform(int expectedActivationStatus, int expectedInputStatus) throws Exception {
		String packageName= adaptPackage(getName());
		IPackageFragment packageFragment= rts.getDefaultSourceFolder().createPackageFragment(packageName, true , null);
		ICompilationUnit cu= createCU(packageFragment, getName());

		ISourceRange selection= getSelection(cu);
		IntroduceParameterRefactoring refactoring= new IntroduceParameterRefactoring(cu, selection.getOffset(), selection.getLength());

		NullProgressMonitor pm= new NullProgressMonitor();
		RefactoringStatus status= refactoring.checkInitialConditions(pm);
		assertEquals("wrong activation status", expectedActivationStatus, status.getSeverity());
		if (! status.isOK())
			return;

		String[] names= getNames(cu);
		if (names == null) {
			refactoring.setParameterName(refactoring.guessedParameterName());
		} else {
			assertEquals("incorrectly guessed parameter name", names[0], refactoring.guessedParameterName());
			refactoring.setParameterName(names[1]);
		}

		status.merge(refactoring.checkFinalConditions(pm));
		assertEquals("wrong input status", expectedInputStatus, status.getSeverity());
		if (status.getSeverity() == RefactoringStatus.FATAL)
			return;

		String out= getProofedContent(packageName + SLASH_OUT, getName());
		performTest(cu, refactoring, out);
	}

// ---

	@Test
	public void testInvalid_NotInMethod1() throws Exception {
		performInvalidSelection();
	}
	@Test
	public void testInvalid_NotInMethod2() throws Exception {
		performInvalidSelection();
	}
	@Test
	public void testInvalid_NotInMethod3() throws Exception {
		performInvalidSelection();
	}

	@Test
	public void testInvalid_PartName1() throws Exception {
		performInvalidSelection();
	}

	@Test
	public void testInvalid_PartString() throws Exception {
		performInvalidSelection();
	}

	@Test
	public void testInvalid_NoMethodBinding() throws Exception {
		performInvalidSelection();
	}

	@Test
	public void testInvalid_NoExpression1() throws Exception {
		performInvalidSelection();
	}

	//	---

	@Test
	public void testSimple_Capture() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_ConstantExpression1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_ConstantExpression2() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Expression1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Expression2() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Expression3() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Expression4() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Expression5() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_NewInstance1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_NewInstanceImport() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_StaticGetter1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Formatting1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Javadoc1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Javadoc2() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Constructor1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Vararg1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Wildcard1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Wildcard2() throws Exception {
		performOK();
	}

	@Test
	public void testSimple_Enum1() throws Exception {
		performOK();
	}
}
