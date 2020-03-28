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

import org.junit.Test;

import org.eclipse.jdt.internal.ui.refactoring.nls.MultiStateCellEditor;

public class CellEditorTester {
	@Test
	public void test0() {
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		assertEquals(ce.getValue(), Integer.valueOf(0));
	}

	@Test
	public void test1() {
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.activate();
		assertEquals(ce.getValue(), Integer.valueOf(1));
	}
	@Test
	public void test2() {
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.activate();
		ce.activate();
		assertEquals(ce.getValue(), Integer.valueOf(2));
	}

	@Test
	public void test3() {
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.activate();
		ce.activate();
		ce.activate();
		assertEquals(ce.getValue(), Integer.valueOf(0));
	}

	@Test
	public void test4() {
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.setValue(Integer.valueOf(1));
		assertEquals(ce.getValue(), Integer.valueOf(1));
	}

	@Test
	public void test5() {
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.setValue(Integer.valueOf(2));
		ce.activate();
		assertEquals(ce.getValue(), Integer.valueOf(0));
	}
}
