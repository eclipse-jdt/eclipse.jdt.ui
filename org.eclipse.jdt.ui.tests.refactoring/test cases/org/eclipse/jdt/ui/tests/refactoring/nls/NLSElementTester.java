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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;

public class NLSElementTester{
	private NLSElement fEl;
	private int fOff, fLen;
	private String fVal;

	@BeforeEach
	public void setUp(){
		fOff= 3;
		fLen= 5;
		fVal= "test"; //$NON-NLS-1$
		fEl= new NLSElement(fVal, fOff, fLen, 0, false);
	}

	@Test
	public void test0() {
		assertEquals(fOff, fEl.getPosition().getOffset(), "Position offset"); //$NON-NLS-1$
	}

	@Test
	public void test1() {
		assertEquals(fLen, fEl.getPosition().getLength(), "Position length"); //$NON-NLS-1$
	}

	@Test
	public void test2() {
		assertEquals(fVal, fEl.getValue(), "value"); //$NON-NLS-1$
	}

	@Test
	public void test3() {
		assertNull(fEl.getTagPosition(), "tagposition"); //$NON-NLS-1$
	}

	@Test
	public void test3a() {
		fEl.setTagPosition(1, 2);
		assertEquals(2, fEl.getTagPosition().getLength(), "tagposition.length"); //$NON-NLS-1$
		assertEquals(1, fEl.getTagPosition().getOffset(), "tagposition.offset"); //$NON-NLS-1$
	}


	@Test
	public void test4() {
		assertFalse(fEl.hasTag(), "hastag"); //$NON-NLS-1$
	}

	@Test
	public void test4a() {
		fEl.setTagPosition(1, 2);
		assertTrue(fEl.hasTag(), "hastag"); //$NON-NLS-1$
	}
}
