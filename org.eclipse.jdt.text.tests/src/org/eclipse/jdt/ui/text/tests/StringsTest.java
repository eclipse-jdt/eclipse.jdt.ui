/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.jdt.internal.corext.util.Strings;

public class StringsTest extends TestCase {


	public StringsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(StringsTest.class);
	}

	
	public void testIsIndentChar() {
		assertTrue(Strings.isIndentChar(' '));
		assertTrue(Strings.isIndentChar('\t'));
		assertTrue(!Strings.isIndentChar('x'));
		assertTrue(!Strings.isIndentChar('\n'));
		assertTrue(!Strings.isIndentChar('\r'));
	}
		
	public void testIsLineDelimiterChar() {
		assertTrue(!Strings.isLineDelimiterChar(' '));
		assertTrue(!Strings.isLineDelimiterChar('\t'));
		assertTrue(!Strings.isLineDelimiterChar('x'));
		assertTrue(Strings.isLineDelimiterChar('\n'));
		assertTrue(Strings.isLineDelimiterChar('\r'));
	}
	
}

