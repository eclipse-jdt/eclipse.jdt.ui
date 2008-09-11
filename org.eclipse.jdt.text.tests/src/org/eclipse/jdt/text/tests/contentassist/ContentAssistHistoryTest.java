/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.java.ContentAssistHistory;
import org.eclipse.jdt.internal.ui.text.java.ContentAssistHistory.RHSHistory;

/**
 * @since 3.2
 */
public class ContentAssistHistoryTest extends TestCase {
	private static final Class THIS= ContentAssistHistoryTest.class;

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


	public ContentAssistHistoryTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS, "ContentAssistHistoryTest")) {
			/*
			 * @see org.eclipse.jdt.ui.tests.core.ProjectTestSetup#setUp()
			 */
			protected void setUp() throws Exception {
				super.setUp();

				IJavaProject project= ProjectTestSetup.getProject();
				fgStringT= project.findType(STRING);
				fgCharSequenceT= project.findType(CHAR_SEQUENCE);
				fgStringBufferT= project.findType(STRING_BUFFER);
				fgCollectionT= project.findType(COLLECTION);
				fgListT= project.findType(LIST);
				fgArrayListT= project.findType(ARRAY_LIST);
				fgLinkedListT= project.findType(LINKED_LIST);
			}
		};
	}

	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return test;
	}

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

	public void testContentAssistHistory() {
		try {
			new ContentAssistHistory();
		} catch (IllegalArgumentException e) {
			fail();
		}
	}

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

	public void testGetHistoryForHierarchy() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		assertEqualMap(map(LIST, list(ARRAY_LIST), COLLECTION, list(ARRAY_LIST), ITERABLE, list(ARRAY_LIST)), history.getEntireHistory());
	}

	public void testGetEntireHistory() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCollectionT, fgLinkedListT);

		assertEqualMap(map(LIST, list(ARRAY_LIST), COLLECTION, list(ARRAY_LIST, LINKED_LIST), ITERABLE, list(ARRAY_LIST, LINKED_LIST)), history.getEntireHistory());
	}

	public void testReadOnlyEntireHistory() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCollectionT, fgLinkedListT);

		Map map= history.getEntireHistory();
		try {
			map.clear();
			fail();
		} catch (UnsupportedOperationException x) {
		}

		try {
			((RHSHistory) map.get(LIST)).getTypes().clear();
			fail();
		} catch (UnsupportedOperationException x) {
		}
	}

	public void testReadOnlyHistory() {
		ContentAssistHistory history= new ContentAssistHistory();

		history.remember(fgListT, fgArrayListT);
		history.remember(fgCollectionT, fgLinkedListT);

		List set= history.getHistory(LIST).getTypes();
		try {
			set.clear();
			fail();
		} catch (UnsupportedOperationException x) {
		}
	}

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

	private static void assertEqualMap(Map expected, Map actual) {
		assertEqualMap("", expected, actual);
	}

	private static void assertEqualMap(String message, Map expected, Map actual) {
		assertEquals(message, expected.size(), actual.size());
		for (Iterator it= expected.keySet().iterator(); it.hasNext();) {
			String type= (String) it.next();
			assertEquals(message, expected.get(type), ((RHSHistory) actual.get(type)).getTypes());
		}
	}

	private Map map(String type, List set) {
		Map map= new HashMap();
		map.put(type, set);
		return map;
	}

	private Map map(String t1, List s1, String t2, List s2) {
		Map map= map(t1, s1);
		map.put(t2, s2);
		return map;
	}

	private Map map(String t1, List s1, String t2, List s2, String t3, List s3) {
		Map map= map(t1, s1, t2, s2);
		map.put(t3, s3);
		return map;
	}

	private Map map(String t1, List s1, String t2, List s2, String t3, List s3, String t4, List s4) {
		Map map= map(t1, s1, t2, s2, t3, s3);
		map.put(t4, s4);
		return map;
	}

	List list(String param) {
		List list= new ArrayList();
		list.add(param);
		return list;
	}

	List list(String p1, String p2) {
		List list= list(p1);
		list.add(p2);
		return list;
	}
}
