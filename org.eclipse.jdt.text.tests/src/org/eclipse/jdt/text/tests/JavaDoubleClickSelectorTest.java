/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickSelector;

/**
 * @since 3.1
 */
public class JavaDoubleClickSelectorTest {
	private static class PublicSelector extends JavaDoubleClickSelector {
		@Override
		public IRegion findWord(IDocument document, int anchor) {
			return super.findWord(document, anchor);
		}
	}

	private void assertSelection(String content, int offset, int selOffset, int selLength) {
		IDocument document= new Document(content);
		PublicSelector selector= new PublicSelector();
		IRegion selection= selector.findWord(document, offset);
		assertEquals(new Region(selOffset, selLength), selection);
	}

	@Test
	public void testIdentifierBefore() throws Exception {
		assertSelection("  foobar  ", 2, 2, 6);
	}

	@Test
	public void testIdentifierInside() throws Exception {
		assertSelection("  foobar  ", 3, 2, 6);
	}

	@Test
	public void testIdentifierBehind() throws Exception {
		assertSelection("  foobar  ", 8, 8, 0);
	}

	@Test
	public void testWhitespaceBefore() throws Exception {
		assertSelection("  foobar  ", 1, 1, 7);
	}

	@Test
	public void testWhitespaceBehind() throws Exception {
		assertSelection("  foobar  ", 9, 9, 0);
	}

	@Test
	public void test15AnnotationBefore() throws Exception {
		assertSelection("  @  Deprecated  ", 2, 2, 13);
	}

	@Test
	public void test15AnnotationInside1() throws Exception {
		assertSelection("  @  Deprecated  ", 3, 2, 13);
	}

	@Test
	public void test15AnnotationInside2() throws Exception {
		assertSelection("  @  Deprecated  ", 4, 2, 13);
	}

	@Test
	public void test15AnnotationInside3() throws Exception {
		assertSelection("  @  Deprecated  ", 5, 2, 13);
	}

	@Test
	public void test15AnnotationInside4() throws Exception {
		assertSelection("  @  Deprecated  ", 6, 2, 13);
	}

	@Test
	public void test15AnnotationBehind() throws Exception {
		assertSelection("  @  Deprecated  ", 15, 15, 0);
	}

	@Test
	public void test15AnnotationNoSpaceBefore() throws Exception {
		assertSelection("  @Deprecated  ", 2, 2, 11);
	}

	@Test
	public void test15AnnotationNoSpaceInside1() throws Exception {
		assertSelection("  @Deprecated  ", 3, 2, 11);
	}

	@Test
	public void test15AnnotationNoSpaceInside2() throws Exception {
		assertSelection("  @Deprecated  ", 4, 2, 11);
	}

	@Test
	public void test15AnnotationNoSpaceBehind() throws Exception {
		assertSelection("  @Deprecated  ", 13, 13, 0);
	}

	@Test
	public void testAnnotationNoIdStartBefore() {
		assertSelection("  @2foobar  ", 2, 2, 0);
	}

	@Test
	public void testAnnotationNoIdStartInside1() {
		assertSelection("  @2foobar  ", 3, 3, 7);
	}

	@Test
	public void testAnnotationNoIdStartInside2() {
		assertSelection("  @2foobar  ", 4, 3, 7);
	}

	@Test
	public void testAnnotationNoIdStartBehind() {
		assertSelection("  @2foobar  ", 10, 10, 0);
	}

	@Test
	public void testBackwardsOnDot() {
		assertSelection("Systen.out", 6, 0, 6);
	}
}
