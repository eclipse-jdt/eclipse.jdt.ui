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

import static org.junit.Assert.fail;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.Rule;
import org.junit.Test;

import org.w3c.dom.Element;

import org.eclipse.jdt.internal.corext.util.History;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class SelectionHistoryTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private void assertEquals(String[] actual, String[] expected) {
		if (expected == null && actual == null)
			return;

		if (expected == null && actual != null) {
			StringBuffer buf= new StringBuffer();
			buf.append("Expected array is null, actual is: ");
			stringArrayToString(actual, buf);
			fail(buf.toString());
		}

		if (expected != null && actual == null) {
			StringBuffer buf= new StringBuffer();
			buf.append("Actual array is null, expected is: ");
			stringArrayToString(expected, buf);
			fail(buf.toString());
		}

		if (expected.length != actual.length) {
			StringBuffer buf= new StringBuffer();
			buf.append("Actual array length is not equal expected array length\n");
			buf.append("Expected: ");
			stringArrayToString(expected, buf);
			buf.append("\nActual: ");
			stringArrayToString(actual, buf);
			fail(buf.toString());
		}

		for (int i= 0; i < actual.length; i++) {
			if (!expected[i].equals(actual[i])) {
				StringBuffer buf= new StringBuffer();
				buf.append("Actual array is not equal expected array \n");
				buf.append("Expected: ");
				stringArrayToString(expected, buf);
				buf.append("\nActual: ");
				stringArrayToString(actual, buf);
				fail(buf.toString());
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

	@Test
	public void organizeImportHistory01() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"d", "c", "b", "a"};
		String[] expected= {"a", "b", "c", "d"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	@Test
	public void organizeImportHistory02() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("a");
		String[] expected= {"a", "b", "c", "d"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	@Test
	public void organizeImportHistory03() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("b");
		String[] expected= {"b", "a", "c", "d"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	@Test
	public void organizeImportHistory04() throws Exception {
		History<String, String> history= new TestHistory();
		Comparator<String> comparator= new TestHistoryComparator(history);

		String[] strings= {"a", "b", "c", "d"};
		history.accessed("b");
		history.accessed("d");
		String[] expected= {"d", "b", "a", "c"};

		Arrays.sort(strings, comparator);
		assertEquals(strings, expected);
	}

	@Test
	public void organizeImportHistory05() throws Exception {
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
