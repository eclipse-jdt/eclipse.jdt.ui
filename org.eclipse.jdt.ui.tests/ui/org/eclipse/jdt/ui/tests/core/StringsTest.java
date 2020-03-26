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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

public class StringsTest {
	@Test
	public void removeTrailingCharacters() {
		assertEquals("x", Strings.removeTrailingCharacters("x", ','));
		assertEquals("x,y", Strings.removeTrailingCharacters("x,y", ','));
		assertEquals("x", Strings.removeTrailingCharacters("x,", ','));
		assertEquals("x", Strings.removeTrailingCharacters("x,,", ','));
		assertEquals("", Strings.removeTrailingCharacters(",", ','));
		assertEquals(",x", Strings.removeTrailingCharacters(",x", ','));
	}
}