/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractConstantRefactoring;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d7Setup;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractConstantTests1d7 extends ExtractConstantTests {
	public ExtractConstantTests1d7() {
		super(new Java1d7Setup());
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract17/" : "cannotExtract17/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	private void failHelper2(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean allowLoadtime, String constantName, String errorMsg, boolean checkMsg) throws Exception{
		ICompilationUnit cu= createCU(getPackageP(), "package-info.java", getFileContents(getTestFileName(false, true)));
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractConstantRefactoring ref= new ExtractConstantRefactoring(cu, selection.getOffset(), selection.getLength());
		ref.setReplaceAllOccurrences(replaceAll);
		ref.setConstantName(constantName);
		RefactoringStatus result= performRefactoring(ref);

		if(!allowLoadtime && !ref.selectionAllStaticFinal())
			return;

		assertNotNull("precondition was supposed to fail", result);
		if(checkMsg)
			assertEquals(errorMsg, result.getEntryMatchingSeverity(RefactoringStatus.FATAL).getMessage());
	}
	//--- TESTS

	// -- testing failing preconditions
	@Override
	@Test
	public void testFail0() throws Exception{
		failHelper1(10, 14, 10, 56, true, true, "CONSTANT");
	}

	@Test
	public void testFailNoType() throws Exception{
		failHelper2(2, 41, 2, 48, true, true, "CONSTANT", RefactoringCoreMessages.ExtractConstantRefactoring_no_type, true);
	}
}
