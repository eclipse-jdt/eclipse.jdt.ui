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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;

public class NLSElementTester{
	private NLSElement fEl;
	private int fOff, fLen;
	private String fVal;

	@Before
	public void setUp(){
		fOff= 3;
		fLen= 5;
		fVal= "test"; //$NON-NLS-1$
		fEl= new NLSElement(fVal, fOff, fLen, 0, false);
	}

	@Test
	public void test0() {
		assertEquals("Position offset", fOff, fEl.getPosition().getOffset()); //$NON-NLS-1$
	}

	@Test
	public void test1() {
		assertEquals("Position length", fLen, fEl.getPosition().getLength()); //$NON-NLS-1$
	}

	@Test
	public void test2() {
		assertEquals("value", fVal, fEl.getValue()); //$NON-NLS-1$
	}

	@Test
	public void test3() {
		assertNull("tagposition", fEl.getTagPosition()); //$NON-NLS-1$
	}

	@Test
	public void test3a() {
		fEl.setTagPosition(1, 2);
		assertEquals("tagposition.length", 2, fEl.getTagPosition().getLength()); //$NON-NLS-1$
		assertEquals("tagposition.offset", 1, fEl.getTagPosition().getOffset()); //$NON-NLS-1$
	}


	@Test
	public void test4() {
		assertFalse("hastag", fEl.hasTag()); //$NON-NLS-1$
	}

	@Test
	public void test4a() {
		fEl.setTagPosition(1, 2);
		assertTrue("hastag", fEl.hasTag()); //$NON-NLS-1$
	}
}
