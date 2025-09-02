/*******************************************************************************
* Copyright (c) 2024 Vector Informatik GmbH and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Vector Informatik GmbH  - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FiltersDialogTest {

	@Test
	void testRegex() throws Exception {
		// Valid java names with globs
		assertTrue(matches("a"));
		assertTrue(matches("a*"));
		assertTrue(matches("*a"));
		assertTrue(matches("a?"));
		assertTrue(matches("_a"));
		assertTrue(matches("?a?"));
		assertTrue(matches("a2"));

		assertTrue(matches("a.n.method"));
		assertTrue(matches("a._n.method"));
		assertTrue(matches("a.n2.method"));
		assertTrue(matches("a.n._method"));
		assertTrue(matches("?a.n._method"));
		assertTrue(matches("?a.?n._method"));

		// Invalid java names
		assertFalse(matches("2a"));

		assertFalse(matches("a.n.method()"));
		assertFalse(matches("a.n.method(y)"));
		assertFalse(matches("a.2n.method"));
		assertFalse(matches("a.2n.?method"));
		assertFalse(matches("a.2n.m*ethod"));
		assertFalse(matches("?a.2n._method"));
		assertFalse(matches("2?a.?n._method"));
	}

	private boolean matches(String string) {
		return string.matches(FiltersDialog.METHOD_NAME_GLOB_PATTERN);
	}
}
