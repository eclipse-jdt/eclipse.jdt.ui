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
package org.eclipse.jdt.ui.tests.refactoring.nls;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.ui.refactoring.nls.MultiStateCellEditor;


public class CellEditorTester extends TestCase {

	public CellEditorTester(String name) {
		super(name);
	}

	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}

	public static Test suite() {
		return new TestSuite(CellEditorTester.class);
	}

	public void test0(){
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		assertTrue(ce.getValue().equals(new Integer(0)));
	}

	public void test1(){
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.activate();
		assertTrue(ce.getValue().equals(new Integer(1)));
	}
	public void test2(){
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.activate();
		ce.activate();
		assertTrue(ce.getValue().equals(new Integer(2)));
	}

	public void test3(){
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.activate();
		ce.activate();
		ce.activate();
		assertTrue(ce.getValue().equals(new Integer(0)));
	}

	public void test4(){
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.setValue(new Integer(1));
		assertTrue(ce.getValue().equals(new Integer(1)));
	}

	public void test5(){
		MultiStateCellEditor ce= new MultiStateCellEditor(null, 3, 0);
		ce.setValue(new Integer(2));
		ce.activate();
		assertTrue(ce.getValue().equals(new Integer(0)));
	}
}


