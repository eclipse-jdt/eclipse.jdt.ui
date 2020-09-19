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

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScanner;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScanner.TextMatch;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RefactoringScannerTests extends GenericRefactoringTest {
	private static class Position {
		private final int fLine;
		private final int fColumn;
		Position(int line, int column) {
			fLine= line;
			fColumn= column;
		}
	}

	public RefactoringScannerTests() {
		rts= new RefactoringTestSetup();
	}

	private RefactoringScanner fScanner;

	@Override
	protected String getRefactoringPath() {
		return "RefactoringScanner/";
	}

	@Override
	public void genericbefore() throws Exception {
		//no need to call super.genericbefore();
		fScanner= new RefactoringScanner("TestPattern", "org.eclipse");
	}

	@Override
	public void genericafter() throws Exception {
		//no need to call super.genericafter();
	}

	private void helper(String fileName, int expectedMatchCount)	throws Exception{
		String text= getFileContents(getRefactoringPath() + fileName);
		fScanner.scan(text);
		assertEquals("results.length", expectedMatchCount, fScanner.getMatches().size());
	}

	private void helper2(String fileName, Position[] expectedMatches)	throws Exception{
		String text= getFileContents(getRefactoringPath() + fileName);
		fScanner.scan(text);

		ArrayList<Integer> expectedMatchesList= new ArrayList<>(expectedMatches.length);
		for (Position expectedMatch : expectedMatches) {
			expectedMatchesList.add(TextRangeUtil.getOffset(text, expectedMatch.fLine, expectedMatch.fColumn));
		}
		ArrayList<Integer> matchesList= new ArrayList<>();
		for (TextMatch element : fScanner.getMatches()) {
			matchesList.add(element.getStartPosition());
		}
		Collections.sort(matchesList);
		assertEquals("results", expectedMatchesList.toString(), matchesList.toString());
	}

	//-- tests
	@Test
	public void test0() throws Exception{
		String text= "";
		fScanner.scan(text);
		assertEquals("results.length", 0, fScanner.getMatches().size());
	}

	@Test
	public void test1() throws Exception{
		helper("A.java", 8);
	}

	@Test
	public void testWord1() throws Exception{
		helper("B.java", 6);
	}

	@Test
	public void testQualifier() throws Exception{
		helper2("C.java", new Position[] {
				new Position(4, 21),
				new Position(17, 21),

				new Position(28, 21),
				new Position(29, 20),

				new Position(32, 20),

				new Position(37, 21),
				new Position(38, 20),
		});
	}
}
