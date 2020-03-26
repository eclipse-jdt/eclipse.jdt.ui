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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.jdt.core.formatter.IndentManipulation;

public class IndentManipulationTest {
	@Test
	public void isIndentChar() {
		assertTrue(IndentManipulation.isIndentChar(' '));
		assertTrue(IndentManipulation.isIndentChar('\t'));
		assertFalse(IndentManipulation.isIndentChar('x'));
		assertFalse(IndentManipulation.isIndentChar('\n'));
		assertFalse(IndentManipulation.isIndentChar('\r'));
	}

	@Test
	public void isLineDelimiterChar() {
		assertFalse(IndentManipulation.isLineDelimiterChar(' '));
		assertFalse(IndentManipulation.isLineDelimiterChar('\t'));
		assertFalse(IndentManipulation.isLineDelimiterChar('x'));
		assertTrue(IndentManipulation.isLineDelimiterChar('\n'));
		assertTrue(IndentManipulation.isLineDelimiterChar('\r'));
	}
}
