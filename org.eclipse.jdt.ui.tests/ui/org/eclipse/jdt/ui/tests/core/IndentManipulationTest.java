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

import org.eclipse.jdt.core.formatter.IndentManipulation;

public class IndentManipulationTest extends TestCase {
	public IndentManipulationTest(String name) {
		super(name);
	}

	public static Test suite() {
		return allTests();
	}

	public static Test allTests() {
		return new TestSuite(IndentManipulationTest.class);
	}

	public void testIsIndentChar() {
		assertTrue(IndentManipulation.isIndentChar(' '));
		assertTrue(IndentManipulation.isIndentChar('\t'));
		assertTrue(!IndentManipulation.isIndentChar('x'));
		assertTrue(!IndentManipulation.isIndentChar('\n'));
		assertTrue(!IndentManipulation.isIndentChar('\r'));
	}

	public void testIsLineDelimiterChar() {
		assertTrue(!IndentManipulation.isLineDelimiterChar(' '));
		assertTrue(!IndentManipulation.isLineDelimiterChar('\t'));
		assertTrue(!IndentManipulation.isLineDelimiterChar('x'));
		assertTrue(IndentManipulation.isLineDelimiterChar('\n'));
		assertTrue(IndentManipulation.isLineDelimiterChar('\r'));
	}
}

