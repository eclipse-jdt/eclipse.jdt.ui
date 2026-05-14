/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.ui.text.JavaWordFinder;

public class JavaWordFinderTest {

	// U+10400 DESERET CAPITAL LETTER LONG I: a supplementary letter that is a
	// valid Java identifier part, encoded as the surrogate pair 𐐀.
	private static final String DESERET_A = "𐐀";

	// U+1F600 GRINNING FACE: a supplementary character that is NOT a valid
	// Java identifier part, encoded as 😀.
	private static final String EMOJI = "😀";

	private static IRegion findWord(String content, int offset) {
		IDocument document = new Document(content);
		return JavaWordFinder.findWord(document, offset);
	}

	// --- basic ASCII cases ---

	@Test
	public void testSimpleIdentifier_inside() {
		assertEquals(new Region(2, 6), findWord("  foobar  ", 4));
	}

	@Test
	public void testSimpleIdentifier_atStart() {
		assertEquals(new Region(2, 6), findWord("  foobar  ", 2));
	}

	@Test
	public void testSimpleIdentifier_atEnd() {
		assertEquals(new Region(8, 0), findWord("  foobar  ", 8));
	}

	@Test
	public void testNonIdentifier_returnsEmptyRegion() {
		assertEquals(new Region(1, 0), findWord("  !!  ", 1));
	}

	// --- supplementary identifier characters (surrogate pairs) ---

	@Test
	public void testSupplementaryIdentifier_offsetOnHighSurrogate() {
		// Document: " <DESERET_A>bc "
		// Indices:   0  1  2  3  4  5
		//            ' ' D8 DC 'b' 'c' ' '
		// Word "𐐀bc" starts at index 1, length 4.
		String content = " " + DESERET_A + "bc ";
		assertEquals(new Region(1, 4), findWord(content, 1));
	}

	@Test
	public void testSupplementaryIdentifier_offsetInsideAsciiPart() {
		// Same document, offset on 'b' (index 3) -- word is still the full identifier.
		String content = " " + DESERET_A + "bc ";
		assertEquals(new Region(1, 4), findWord(content, 3));
	}

	@Test
	public void testSupplementaryIdentifierAtEnd_offsetInAsciiPart() {
		// Document: " abc<DESERET_A> "
		// Indices:   0  1  2  3  4  5  6
		//            ' ' 'a' 'b' 'c' D8 DC ' '
		// Word "abc𐐀" starts at 1, length 5.
		String content = " abc" + DESERET_A + " ";
		assertEquals(new Region(1, 5), findWord(content, 2));
	}

	// --- non-identifier supplementary characters (emoji) ---

	@Test
	public void testNonIdentifierSurrogate_breaksWord() {
		// Document: " <EMOJI>bc "
		// The emoji is not a Java identifier part, so "bc" is its own word.
		// offset on 'b' (index 3).
		String content = " " + EMOJI + "bc ";
		assertEquals(new Region(3, 2), findWord(content, 3));
	}

	@Test
	public void testNonIdentifierSurrogate_betweenAsciiWords() {
		// Document: "ab<EMOJI>cd"
		// Indices:   0  1  2  3  4  5
		//            'a' 'b' D8 DE 'c' 'd'
		// Offset on 'c' (index 4): word is "cd" = Region(4, 2).
		// Offset on 'b' (index 1): word is "ab" = Region(0, 2).
		String content = "ab" + EMOJI + "cd";
		assertEquals(new Region(4, 2), findWord(content, 4));
		assertEquals(new Region(0, 2), findWord(content, 1));
	}
}
