/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.ui.dialogs.SelectionHistory;


public class SelectionHistoryTest extends TestCase {
	
	private static final Class THIS= SelectionHistoryTest.class;
	
	public SelectionHistoryTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		}
		return setUpTest(new SelectionHistoryTest("testOrganizeImportHistory01"));
	}
	
	private void assertEquals(String[] actual, String[] expected) {
		if (expected == null && actual == null)
			return;
		
		if (expected == null && actual != null) {
			StringBuffer buf= new StringBuffer();
			buf.append("Expected array is null, actual is: ");
			stringArrayToString(actual, buf);
			assertTrue(buf.toString(), false);
		}
		
		if (expected != null && actual == null) {
			StringBuffer buf= new StringBuffer();
			buf.append("Actual array is null, expected is: ");
			stringArrayToString(expected, buf);
			assertTrue(buf.toString(), false);
		}
		
		if (expected.length != actual.length) {
			StringBuffer buf= new StringBuffer();
			buf.append("Actual array length is not equal expected array length\n");
			buf.append("Expected: ");
			stringArrayToString(expected, buf);
			buf.append("\nActual: ");
			stringArrayToString(actual, buf);
			assertTrue(buf.toString(), false);
		}
		
		for (int i= 0; i < actual.length; i++) {
			if (!expected[i].equals(actual[i])) {
				StringBuffer buf= new StringBuffer();
				buf.append("Actual array is not equal expected array \n");
				buf.append("Expected: ");
				stringArrayToString(expected, buf);
				buf.append("\nActual: ");
				stringArrayToString(actual, buf);
				assertTrue(buf.toString(), false);
			}
		}
	}
	
	private void stringArrayToString(String[] actual, StringBuffer buf) {
		buf.append('{');
		if (actual.length > 0) {
			buf.append(actual[0]);
			for (int i= 1; i < actual.length; i++) {
				buf.append(", ");
				buf.append(actual[i]);
			}
		}
		buf.append('}');
	}

	public void testOrganizeImportHistory01() throws Exception {
		SelectionHistory history= SelectionHistory.getInstance(SelectionHistory.ORGANIZE_IMPORT_ID);
		history.clear();
		
		String[] strings= {"d", "c", "b", "a"};
		String[] expected= {"a", "b", "c", "d"};
		
		Arrays.sort(strings, history.getComparator());
		assertEquals(strings, expected);
	}
	
	public void testOrganizeImportHistory02() throws Exception {
		SelectionHistory history= SelectionHistory.getInstance(SelectionHistory.ORGANIZE_IMPORT_ID);
		history.clear();
		
		String[] strings= {"a", "b", "c", "d"};
		history.remember("a");
		String[] expected= {"a", "b", "c", "d"};
		
		Arrays.sort(strings, history.getComparator());
		assertEquals(strings, expected);
	}
	
	public void testOrganizeImportHistory03() throws Exception {
		SelectionHistory history= SelectionHistory.getInstance(SelectionHistory.ORGANIZE_IMPORT_ID);
		history.clear();
		
		String[] strings= {"a", "b", "c", "d"};
		history.remember("b");
		String[] expected= {"b", "a", "c", "d"};
		
		Arrays.sort(strings, history.getComparator());
		assertEquals(strings, expected);
	}
	
	public void testOrganizeImportHistory04() throws Exception {
		SelectionHistory history= SelectionHistory.getInstance(SelectionHistory.ORGANIZE_IMPORT_ID);
		history.clear();
		
		String[] strings= {"a", "b", "c", "d"};
		history.remember("b");
		history.remember("d");
		String[] expected= {"d", "b", "a", "c"};
		
		Arrays.sort(strings, history.getComparator());
		assertEquals(strings, expected);
	}
	
	public void testOrganizeImportHistory05() throws Exception {
		SelectionHistory history= SelectionHistory.getInstance(SelectionHistory.ORGANIZE_IMPORT_ID);
		history.clear();
		
		String[] strings= {"a", "b", "c", "d"};
		history.remember("b");
		history.remember("d");
		history.remember("b");
		String[] expected= {"b", "d", "a", "c"};
		
		Arrays.sort(strings, history.getComparator());
		assertEquals(strings, expected);
	}

}
