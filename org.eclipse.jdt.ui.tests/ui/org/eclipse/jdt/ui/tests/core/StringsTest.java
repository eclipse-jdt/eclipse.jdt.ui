/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

public class StringsTest extends TestCase {

	public static Test suite() {
		return new TestSuite(StringsTest.class);
	}

	public void testRemoveTrailingCharacters() {
		assertEquals("x", Strings.removeTrailingCharacters("x", ','));
		assertEquals("x,y", Strings.removeTrailingCharacters("x,y", ','));
		assertEquals("x", Strings.removeTrailingCharacters("x,", ','));
		assertEquals("x", Strings.removeTrailingCharacters("x,,", ','));
		assertEquals("", Strings.removeTrailingCharacters(",", ','));
		assertEquals(",x", Strings.removeTrailingCharacters(",x", ','));
	}
}