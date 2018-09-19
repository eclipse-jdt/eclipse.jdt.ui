/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

import org.w3c.dom.Element;

import org.eclipse.jdt.internal.corext.util.History;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class SelectionHistoryTest extends TestCase {

	private static final Class<SelectionHistoryTest> THIS= SelectionHistoryTest.class;

	public SelectionHistoryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
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

	private static final class TestHistoryComparator implements Comparator<String> {

		private final History<String, String> fHistory;

		public TestHistoryComparator(History<String, String> history) {
			fHistory= history;
		}

		@Override
		public int compare(String o1, String o2) {
			int pos1= fHistory.getPosition(o1);
			int pos2= fHistory.getPosition(o2);

			if (pos1 == pos2)
				return Collator.getInstance().compare(o1, o2);

			if (pos1 > pos2) {
				return -1;
			} else {
				return 1;
			}
		}

	}

	private static final class TestHistory extends History<String, String> {

		public TestHistory() {
			super("");
		}
		@Override
		protected void setAttributes(Object object, Element element) {}
		@Override
		protected String createFromElement(Element type) {return null;}
		@Override
		protected String getKey(String object) {return object;}

	}

	public void testOrganizeImportHistory01() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"d", "c", "b", "a"};
		String[] expected= {"a", "b", "c", "d"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	public void testOrganizeImportHistory02() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("a");
		String[] expected= {"a", "b", "c", "d"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	public void testOrganizeImportHistory03() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("b");
		String[] expected= {"b", "a", "c", "d"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	public void testOrganizeImportHistory04() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("b");
		history.accessed("d");
		String[] expected= {"d", "b", "a", "c"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	public void testOrganizeImportHistory05() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("b");
		history.accessed("d");
		history.accessed("b");
		String[] expected= {"b", "d", "a", "c"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

}
