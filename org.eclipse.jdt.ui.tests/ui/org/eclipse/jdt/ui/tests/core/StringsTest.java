/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.util.Strings;

public class StringsTest extends TestCase {

	public static Test allTests() {
		return new TestSuite(StringsTest.class);
	}

	public static Test suite() {
		return allTests();
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