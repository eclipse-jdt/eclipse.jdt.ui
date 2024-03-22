/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.internal.ui.text.JavaBreakIterator;

/**
 * @since 3.0
 */
public class JavaBreakIteratorTest extends BreakIteratorTest {

	@Before
	public void setUp() throws Exception {
		fBreakIterator= new JavaBreakIterator();
	}

	@Test
	public void testNext1() {
		assertNextPositions("word word", new int[] { 4, 5, 9 });
	}

	@Test
	public void testNext2() {
		assertNextPositions("wordWord word", new int[] { 4, 8, 9, 13 });
	}

	@Test
	public void testNextSpace() {
		assertNextPositions(" word ", new int[] { 1, 5, 6 });
	}

	@Test
	public void testNextParen() {
		assertNextPositions("word(params)", new int[] { 4, 5, 11, 12 });
	}

	@Test
	public void testNextLn() {
		String s= "word \n" +
				"  word2";
		assertNextPositions(s, new int[] { 4, 5, 6, 8, 13 });
	}

	@Test
	public void testMultiNextLn() {
		String s= """
			word\s
			
			
			  word2""";
		assertNextPositions(s, new int[] { 4, 5, 6, 7, 8, 10, 15 });
	}

	@Test
	public void testMultiNextLn2() {
		String s= """
			word \r
			\r
			\r
			  word2""";
		assertNextPositions(s, new int[] { 4, 5, 7, 9, 11, 13, 18 });
	}

	@Test
	public void testNextCamelCaseWord() {
		String s= "   _isURLConnection_   ";
		assertNextPositions(s, new int[] { 3, 6, 9, 20, 23 });
	}

	@Test
	public void testPrevious1() {
		String s= "word word";
		assertPreviousPositions(s, new int[] { 0, 4, 5 });
	}

	@Test
	public void testPrevious2() {
		String s= "wordWord word";
		assertPreviousPositions(s, new int[] { 0, 4, 8, 9 });
	}

	@Test
	public void testPreviousSpace() {
		String s= " word ";
		assertPreviousPositions(s, new int[] { 1, 5 });
	}

	@Test
	public void testPreviousParen() {
		String s= "word(params)";
		assertPreviousPositions(s, new int[] { 0, 4, 5, 11 });
	}

	@Test
	public void testPreviousLn() {
		String s= "word \n" +
				"  word2";
		assertPreviousPositions(s, new int[] { 0, 4, 5, 6, 8 });
	}

	@Test
	public void testMultiPreviousLn() {
		String s= """
			word\s
			
			
			  word2""";
		assertPreviousPositions(s, new int[] { 0, 4, 5, 6, 7, 8, 10 });
	}

	@Test
	public void testMultiPreviousLn2() {
		String s= """
			word \r
			\r
			\r
			  word2""";
		assertPreviousPositions(s, new int[] { 0, 4, 5, 7, 9, 11, 13 });
	}

	@Test
	public void testPreviousCamelCaseWord() {
		String s= "   _isURLConnection_   ";
		assertPreviousPositions(s, new int[] { 0, 3, 6, 9, 20 });
	}
}
