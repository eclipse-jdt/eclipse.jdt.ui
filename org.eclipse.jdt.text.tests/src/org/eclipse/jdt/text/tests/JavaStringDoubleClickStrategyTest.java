/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Thomas Wolf - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.text.java.JavaStringDoubleClickStrategy;

public class JavaStringDoubleClickStrategyTest {

	/**
	 * Makes link {@link #findWord(IDocument, int)} accessible and provides a way to fake the
	 * partition region.
	 */
	private static class TestStrategy extends JavaStringDoubleClickStrategy {

		private final IRegion partition;

		public TestStrategy(IRegion partition, int delimiterLength) {
			super("Dummy", delimiterLength, delimiterLength, delimiterLength);
			this.partition= partition;
		}

		@Override
		protected IRegion getPartitionRegion(IDocument document, int offset) throws BadLocationException {
			if (partition != null) {
				return partition;
			}
			return new Region(0, document.getLength());
		}

		@Override
		protected IRegion findWord(IDocument document, int offset) {
			return super.findWord(document, offset);
		}
	}

	private void assertDoubleClick(String text, int offset, IRegion expected) {
		assertDoubleClick(text, offset, expected, (IRegion) null);
	}

	private void assertDoubleClick(String text, int offset, IRegion expected, IRegion partition) {
		assertDoubleClick(text, offset, expected, partition, 1);
	}

	private void assertDoubleClick(String text, int offset, IRegion expected, IRegion partition, int delimiterLength) {
		IDocument document= new Document(text);
		TestStrategy strategy= new TestStrategy(partition, delimiterLength);
		IRegion selection= strategy.findWord(document, offset);
		assertEquals("Click at " + offset + " in: " + text, expected, selection);
	}

	private void assertDoubleClick(String text, int offset, IRegion expected, String version) {
		IDocument document= new Document(text);
		TestStrategy strategy= new TestStrategy(null, 1);
		strategy.setSourceVersion(version);
		IRegion selection= strategy.findWord(document, offset);
		assertEquals("Click at " + offset + " in: " + text, expected, selection);
	}

	@Test
	public void testSingleWordString() {
		assertDoubleClick("\"bye\"", 3, new Region(1, 3));
		assertDoubleClick("\"bye\"", 2, new Region(1, 3));
		assertDoubleClick("\"bye\"", 1, new Region(1, 3));
	}

	@Test
	public void testWithSimpleEscape() {
		assertDoubleClick("\"bye\\nbye\"", 9, new Region(6, 3));
		assertDoubleClick("\"bye\\nbye\"", 8, new Region(6, 3));
		assertDoubleClick("\"bye\\nbye\"", 7, new Region(6, 3));
		assertDoubleClick("\"bye\\nbye\"", 6, new Region(4, 2));
		assertDoubleClick("\"bye\\nbye\"", 5, new Region(4, 2));
		assertDoubleClick("\"bye\\nbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\nbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testWithDoubleEscape() {
		assertDoubleClick("\"bye\\\\nbye\"", 10, new Region(6, 4));
		assertDoubleClick("\"bye\\\\nbye\"", 9, new Region(6, 4));
		assertDoubleClick("\"bye\\\\nbye\"", 8, new Region(6, 4));
		assertDoubleClick("\"bye\\\\nbye\"", 7, new Region(6, 4));
		assertDoubleClick("\"bye\\\\nbye\"", 6, new Region(6, 4));
		assertDoubleClick("\"bye\\\\nbye\"", 5, new Region(4, 2));
		assertDoubleClick("\"bye\\\\nbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\\\nbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testWithTripleEscape() {
		assertDoubleClick("\"bye\\\\\\nbye\"", 11, new Region(8, 3));
		assertDoubleClick("\"bye\\\\\\nbye\"", 10, new Region(8, 3));
		assertDoubleClick("\"bye\\\\\\nbye\"", 9, new Region(8, 3));
		assertDoubleClick("\"bye\\\\\\nbye\"", 8, new Region(6, 2));
		assertDoubleClick("\"bye\\\\\\nbye\"", 7, new Region(6, 2));
		assertDoubleClick("\"bye\\\\\\nbye\"", 6, new Region(4, 2));
		assertDoubleClick("\"bye\\\\\\nbye\"", 5, new Region(4, 2));
		assertDoubleClick("\"bye\\\\\\nbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\\\\\nbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testWithUnicodeEscape1() {
		assertDoubleClick("\"bye\\u004fbye\"", 13, new Region(10, 3));
		assertDoubleClick("\"bye\\u004fbye\"", 12, new Region(10, 3));
		assertDoubleClick("\"bye\\u004fbye\"", 11, new Region(10, 3));
		assertDoubleClick("\"bye\\u004fbye\"", 10, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fbye\"", 9, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fbye\"", 8, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fbye\"", 7, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fbye\"", 6, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fbye\"", 5, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\u004fbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testWithUnicodeEscape2() {
		assertDoubleClick("\"bye\\u004fnbye\"", 14, new Region(10, 4));
		assertDoubleClick("\"bye\\u004fnbye\"", 13, new Region(10, 4));
		assertDoubleClick("\"bye\\u004fnbye\"", 12, new Region(10, 4));
		assertDoubleClick("\"bye\\u004fnbye\"", 11, new Region(10, 4));
		assertDoubleClick("\"bye\\u004fnbye\"", 10, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fnbye\"", 9, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fnbye\"", 8, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fnbye\"", 7, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fnbye\"", 6, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fnbye\"", 5, new Region(4, 6));
		assertDoubleClick("\"bye\\u004fnbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\u004fnbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testWithBrokenUnicodeEscape() {
		assertDoubleClick("\"bye\\u004nbye\"", 13, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 12, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 11, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 10, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 9, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 8, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 7, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 6, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 5, new Region(5, 8));
		assertDoubleClick("\"bye\\u004nbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\u004nbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testWithPartition() {
		assertDoubleClick("hello \"bye\\nbye\" world", 21, new Region(17, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 20, new Region(17, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 19, new Region(17, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 18, new Region(17, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 17, new Region(17, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 16, new Region(15, 1), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 15, new Region(12, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 14, new Region(12, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 13, new Region(12, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 12, new Region(10, 2), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 11, new Region(10, 2), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 10, new Region(7, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 9, new Region(7, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 8, new Region(7, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 7, new Region(7, 3), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 6, new Region(5, 1), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 5, new Region(0, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 4, new Region(0, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 3, new Region(0, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 2, new Region(0, 5), new Region(6, 10));
		assertDoubleClick("hello \"bye\\nbye\" world", 1, new Region(0, 5), new Region(6, 10));
	}

	@Test
	public void testOctalEscape() {
		assertDoubleClick("\"\"\"\nbye\\0\n\"\"\"", 8, new Region(7, 2), null, 3);
		assertDoubleClick("\"\"\"\nbye\\0\n\"\"\"", 7, new Region(4, 3), null, 3);
		assertDoubleClick("\"\"\"\nbye\\01\n\"\"\"", 9, new Region(7, 3), null, 3);
		assertDoubleClick("\"\"\"\nbye\\01\n\"\"\"", 8, new Region(7, 3), null, 3);
		assertDoubleClick("\"\"\"\nbye\\01\n\"\"\"", 7, new Region(4, 3), null, 3);
		assertDoubleClick("\"\"\"\nbye\\012\n\"\"\"", 10, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\012\n\"\"\"", 9, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\012\n\"\"\"", 8, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\012\n\"\"\"", 7, new Region(4, 3), null, 3);
		assertDoubleClick("\"\"\"\nbye\\0123\n\"\"\"", 11, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\0123\n\"\"\"", 10, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\0123\n\"\"\"", 9, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\0123\n\"\"\"", 8, new Region(7, 4), null, 3);
		assertDoubleClick("\"\"\"\nbye\\0123\n\"\"\"", 7, new Region(4, 3), null, 3);
	}

	@Test
	public void testLineTerminator() {
		assertDoubleClick("\"\"\"\nbye\\\n\"\"\"", 7, new Region(4, 3), null, 3);
	}

	@Test
	public void testSpaceEscapeDefault() {
		assertDoubleClick("\"bye\\sbye\"", 9, new Region(5, 4));
		assertDoubleClick("\"bye\\sbye\"", 8, new Region(5, 4));
		assertDoubleClick("\"bye\\sbye\"", 7, new Region(5, 4));
		assertDoubleClick("\"bye\\sbye\"", 6, new Region(5, 4));
		assertDoubleClick("\"bye\\sbye\"", 5, new Region(5, 4));
		assertDoubleClick("\"bye\\sbye\"", 4, new Region(1, 3));
		assertDoubleClick("\"bye\\sbye\"", 3, new Region(1, 3));
	}

	@Test
	public void testSpaceEscapeOnJava11() {
		assertDoubleClick("\"bye\\sbye\"", 9, new Region(5, 4), JavaCore.VERSION_11);
		assertDoubleClick("\"bye\\sbye\"", 8, new Region(5, 4), JavaCore.VERSION_11);
		assertDoubleClick("\"bye\\sbye\"", 7, new Region(5, 4), JavaCore.VERSION_11);
		assertDoubleClick("\"bye\\sbye\"", 6, new Region(5, 4), JavaCore.VERSION_11);
		assertDoubleClick("\"bye\\sbye\"", 5, new Region(5, 4), JavaCore.VERSION_11);
		assertDoubleClick("\"bye\\sbye\"", 4, new Region(1, 3), JavaCore.VERSION_11);
		assertDoubleClick("\"bye\\sbye\"", 3, new Region(1, 3), JavaCore.VERSION_11);
	}

	@Test
	public void testSpaceEscapeOnJava15() {
		assertDoubleClick("\"bye\\sbye\"", 9, new Region(6, 3), JavaCore.VERSION_15);
		assertDoubleClick("\"bye\\sbye\"", 8, new Region(6, 3), JavaCore.VERSION_15);
		assertDoubleClick("\"bye\\sbye\"", 7, new Region(6, 3), JavaCore.VERSION_15);
		assertDoubleClick("\"bye\\sbye\"", 6, new Region(4, 2), JavaCore.VERSION_15);
		assertDoubleClick("\"bye\\sbye\"", 5, new Region(4, 2), JavaCore.VERSION_15);
		assertDoubleClick("\"bye\\sbye\"", 4, new Region(1, 3), JavaCore.VERSION_15);
		assertDoubleClick("\"bye\\sbye\"", 3, new Region(1, 3), JavaCore.VERSION_15);
	}

	@Test
	public void testSpaceEscapeOnJava16() {
		assertDoubleClick("\"bye\\sbye\"", 9, new Region(6, 3), JavaCore.VERSION_16);
		assertDoubleClick("\"bye\\sbye\"", 8, new Region(6, 3), JavaCore.VERSION_16);
		assertDoubleClick("\"bye\\sbye\"", 7, new Region(6, 3), JavaCore.VERSION_16);
		assertDoubleClick("\"bye\\sbye\"", 6, new Region(4, 2), JavaCore.VERSION_16);
		assertDoubleClick("\"bye\\sbye\"", 5, new Region(4, 2), JavaCore.VERSION_16);
		assertDoubleClick("\"bye\\sbye\"", 4, new Region(1, 3), JavaCore.VERSION_16);
		assertDoubleClick("\"bye\\sbye\"", 3, new Region(1, 3), JavaCore.VERSION_16);
	}
}
