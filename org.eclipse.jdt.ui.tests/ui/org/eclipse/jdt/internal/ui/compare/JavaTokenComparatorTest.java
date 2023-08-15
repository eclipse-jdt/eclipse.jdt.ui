/*******************************************************************************
 * Copyright (c) 2023 SAP and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.Test;

public class JavaTokenComparatorTest {

	@Test
	public void stringLiteralSplittedInIncludeWhitespacesConfig() {
		var left= new JavaTokenComparator("a = \"test\";", false);
		var right= new JavaTokenComparator("ab = \"test\";", false);
		assertEquals("\"".length(), left.getTokenLength(4), "string literal token splitted in include whitespaces config");
		assertEquals("\"".length(), right.getTokenLength(4), "string literal token splitted in include whitespaces config");
		assertTrue(left.rangesEqual(4, right, 4));
	}

	@Test
	public void stringLiteralNotSplittedInIgnoreWhitespacesConfig() {
		var left= new JavaTokenComparator("a = \"test\";", true);
		var right= new JavaTokenComparator("ab = \"test\";", true);
		assertEquals("\"test\"".length(), left.getTokenLength(4), "string literal token not splitted in ignore whitespaces config");
		assertEquals("\"test\"".length(), right.getTokenLength(4), "string literal token not splitted in ignore whitespaces config");
		assertTrue(left.rangesEqual(4, right, 4));
	}

	@Test
	public void rangesEqualInIncludeWhitespacesConfig() {
		var left= new JavaTokenComparator("a = \"test\";", false);
		var right= new JavaTokenComparator("ab = \"test \";", false);
		assertEquals("test".length(), left.getTokenLength(5), "string literal token splitted in include whitespaces config");
		assertEquals("test".length(), right.getTokenLength(5), "string literal token splitted in include whitespaces config");
		assertTrue(left.rangesEqual(5, right, 5));
	}

	@Test
	public void rangesNotEqualInIgnoreWhitespacesConfig() {
		var left= new JavaTokenComparator("a = \"test\";", true);
		var right= new JavaTokenComparator("ab = \"test \";", true);
		assertEquals("\"test\"".length(), left.getTokenLength(4), "string literal token splitted in include whitespaces config");
		assertEquals("\"test \"".length(), right.getTokenLength(4), "string literal token splitted in include whitespaces config");
		assertFalse(left.rangesEqual(4, right, 4));
	}
}
