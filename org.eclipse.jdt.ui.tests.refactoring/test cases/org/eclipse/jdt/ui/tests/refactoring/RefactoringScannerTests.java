/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScanner;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScanner.TextMatch;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RefactoringScannerTests extends RefactoringTest{

	private static class Position {
		private final int fLine;
		private final int fColumn;
		Position(int line, int column) {
			fLine= line;
			fColumn= column;
		}
	}


	public RefactoringScannerTests(String name){
		super(name);
	}

	private RefactoringScanner fScanner;
	private static Class<RefactoringScannerTests> clazz= RefactoringScannerTests.class;

	@Override
	protected String getRefactoringPath() {
		return "RefactoringScanner/";
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	@Override
	protected void setUp() throws Exception {
		//no need to call super.setUp();
		fScanner= new RefactoringScanner("TestPattern", "org.eclipse");
	}

	@Override
	protected void tearDown() throws Exception {
		//no need to call super.tearDown();
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
		for (int i= 0; i < expectedMatches.length; i++)
			expectedMatchesList.add(Integer.valueOf(TextRangeUtil.getOffset(text, expectedMatches[i].fLine, expectedMatches[i].fColumn)));
		ArrayList<Integer> matchesList= new ArrayList<>();
		Set<TextMatch> matches= fScanner.getMatches();
		for (Iterator<TextMatch> iter= matches.iterator(); iter.hasNext();) {
			TextMatch element= iter.next();
			matchesList.add(Integer.valueOf(element.getStartPosition()));
		}
		Collections.sort(matchesList);
		assertEquals("results", expectedMatchesList.toString(), matchesList.toString());
	}

	//-- tests
	public void test0() throws Exception{
		String text= "";
		fScanner.scan(text);
		assertEquals("results.length", 0, fScanner.getMatches().size());
	}

	public void test1() throws Exception{
		helper("A.java", 8);
	}

	public void testWord1() throws Exception{
		helper("B.java", 6);
	}

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

