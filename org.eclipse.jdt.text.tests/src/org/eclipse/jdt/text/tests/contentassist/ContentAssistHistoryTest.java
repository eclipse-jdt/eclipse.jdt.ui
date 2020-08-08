/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.java.ContentAssistHistory;
import org.eclipse.jdt.internal.ui.text.java.ContentAssistHistory.RHSHistory;

/**
 * @since 3.2
 */
public class ContentAssistHistoryTest {
	private static final String LINKED_LIST= "java.util.LinkedList";
	private static final String ARRAY_LIST= "java.util.ArrayList";
	private static final String ITERABLE= "java.lang.Iterable";
	private static final String LIST= "java.util.List";
	private static final String COLLECTION= "java.util.Collection";
	private static final String STRING_BUFFER= "java.lang.StringBuffer";
	private static final String CHAR_SEQUENCE= "java.lang.CharSequence";
	private static final String STRING= "java.lang.String";

	private static IType fgStringT;
	private static IType fgCharSequenceT;
	private static IType fgStringBufferT;
	private static IType fgCollectionT;
	private static IType fgListT;
	private static IType fgArrayListT;
	private static IType fgLinkedListT;

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup() {
			/*
			 * @see org.eclipse.jdt.ui.tests.core.ProjectTestSetup#setUp()
			 */
			@Override
			protected void before() throws Throwable {
				super.before();

				IJavaProject project= getProject();
				fgStringT= project.findType(STRING);
				fgCharSequenceT= project.findType(CHAR_SEQUENCE);
				fgStringBufferT= project.findType(STRING_BUFFER);
				fgCollectionT= project.findType(COLLECTION);
				fgListT= project.findType(LIST);
				fgArrayListT= project.findType(ARRAY_LIST);
				fgLinkedListT= project.findType(LINKED_LIST);
			}
		};

	@Test
	public void testContentAssistHistoryIntInt() {
		try {
			new ContentAssistHistory(-1, 1);
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			new ContentAssistHistory(1, 0);
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			new ContentAssistHistory(1, 1);
		} catch (IllegalArgumentException e) {
			fail();
		}
	}

	@Test
	public void testContentAssistHistory() {
		try {
			new ContentAssistHistory();
		} catch (IllegalArgumentException e) {
			fail();
		}
	}

	@Test
	public void testRememberArgumentChecking() throws Exception {
		ContentAssistHistory history= new ContentAssistHistory();
		try {
			history.remember(null, null);
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			history.remember(null, fgStringT);
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			history.remember(fgStringT, null);
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			history.remember(fgStringT, fgStringT);
		} catch (IllegalArgumentException e) {
			fail();
		}
	}

	@Test
	public void testGetHistory() {
		ContentAssistHistory history= new ContentAssistHistory();
		assertTrue(history.getHistory(STRING).getTypes().isEmpty());

		history.remember(fgCharSequenceT, fgStringT);
		assertTrue(history.getHistory(STRING).getTypes().isEmpty());
		assertEquals(list(STRING), history.getHistory(CHAR_SEQUENCE).getTypes());

		history.remember(fgStringT, fgStringT);
		assertTrue("history must not remember final left hand sides", history.getHistory(STRING).getTypes().isEmpty());

		history.remember(fgCharSequenceT, fgStringBufferT);
		assertTrue(history.getHistory(STRING).getTypes().isEmpty());
		assertEquals(list(STRING, STRING_BUFFER), history.getHistory(CHAR_SEQUENCE).getTypes());

		history.remember(fgStringT, fgStringT);
		assertTrue(history.getHistory(STRING).getTypes().isEmpty());
		assertEquals("order not correct", list(STRING_BUFFER, STRING), history.getHistory(CHAR_SEQUENCE).getTypes());
	}

	@Test
	public void testHistoryCapSize() {
		ContentAssistHistory history= new ContentAssistHistory(1, 1);

		history.remember(fgCharSequenceT, fgStringT);
		assertEqualMap(map(CHAR_SEQUENCE, list(STRING)), history.getEntireHistory());

		history.remember(fgStringT, fgStringT);
		assertEqualMap("adding final types must not modify the history", map(CHAR_SEQUENCE, list(STRING)), history.getEntireHistory());

		history.remember(fgCharSequenceT, fgStringBufferT);
		assertEqualMap(map(CHAR_SEQUENCE, list(STRING_BUFFER)), history.getEntireHistory());

		history.remember(fgCharSequenceT, fgArrayListT);
		assertEqualMap("adding types that are not related must not modify the history", map(CHAR_SEQUENCE, list(STRING_BUFFER)), history.getEntireHistory());

		history.remember(fgListT, fgStringT);
		assertEqualMap("adding types that are not related must not modify the history", map(CHAR_SEQUENCE, list(STRING_BUFFER)), history.getEntireHistory());

		history.remember(fgCollectionT, fgArrayListT);
		assertEqualMap(map(COLLECTION, list(ARRAY_LIST)), history.getEntireHistory());
	}

	@Test
	public void testGetHistoryForHierarchy() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		assertEqualMap(map(LIST, list(ARRAY_LIST), COLLECTION, list(ARRAY_LIST), ITERABLE, list(ARRAY_LIST)), history.getEntireHistory());
	}

	@Test
	public void testGetEntireHistory() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCollectionT, fgLinkedListT);

		assertEqualMap(map(LIST, list(ARRAY_LIST), COLLECTION, list(ARRAY_LIST, LINKED_LIST), ITERABLE, list(ARRAY_LIST, LINKED_LIST)), history.getEntireHistory());
	}

	@Test
	public void testReadOnlyEntireHistory() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCollectionT, fgLinkedListT);

		Map<String, RHSHistory> map= history.getEntireHistory();
		try {
			map.clear();
			fail();
		} catch (UnsupportedOperationException x) {
		}

		try {
			map.get(LIST).getTypes().clear();
			fail();
		} catch (UnsupportedOperationException x) {
		}
	}

	@Test
	public void testReadOnlyHistory() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCollectionT, fgLinkedListT);

		List<String> set= history.getHistory(LIST).getTypes();
		try {
			set.clear();
			fail();
		} catch (UnsupportedOperationException x) {
		}
	}

	@Test
	public void testLoadStore() throws Exception {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCharSequenceT, fgStringT);


		Preferences prefs= new Preferences();
		String key= "myKey";
		ContentAssistHistory.store(history, prefs, key);
		ContentAssistHistory loaded= ContentAssistHistory.load(prefs, key);

		assertEqualMap(map(LIST, list(ARRAY_LIST), COLLECTION, list(ARRAY_LIST), ITERABLE, list(ARRAY_LIST), CHAR_SEQUENCE, list(STRING)), loaded.getEntireHistory());
	}

	private static void assertEqualMap(Map<String, List<String>> expected, Map<String, RHSHistory> actual) {
		assertEqualMap("", expected, actual);
	}

	private static void assertEqualMap(String message, Map<String, List<String>> expected, Map<String, RHSHistory> actual) {
		assertEquals(message, expected.size(), actual.size());
		for (Map.Entry<String, List<String>> entry : expected.entrySet()) {
			String type = entry.getKey();
			assertEquals(message, entry.getValue(), actual.get(type).getTypes());
		}
	}

	private Map<String, List<String>> map(String type, List<String> set) {
		Map<String, List<String>> map= new HashMap<>();
		map.put(type, set);
		return map;
	}

	private Map<String, List<String>> map(String t1, List<String> s1, String t2, List<String> s2) {
		Map<String, List<String>> map= map(t1, s1);
		map.put(t2, s2);
		return map;
	}

	private Map<String, List<String>> map(String t1, List<String> s1, String t2, List<String> s2, String t3, List<String> s3) {
		Map<String, List<String>> map= map(t1, s1, t2, s2);
		map.put(t3, s3);
		return map;
	}

	private Map<String, List<String>> map(String t1, List<String> s1, String t2, List<String> s2, String t3, List<String> s3, String t4, List<String> s4) {
		Map<String, List<String>> map= map(t1, s1, t2, s2, t3, s3);
		map.put(t4, s4);
		return map;
	}

	List<String> list(String param) {
		List<String> list= new ArrayList<>();
		list.add(param);
		return list;
	}

	List<String> list(String p1, String p2) {
		List<String> list= list(p1);
		list.add(p2);
		return list;
	}
}
