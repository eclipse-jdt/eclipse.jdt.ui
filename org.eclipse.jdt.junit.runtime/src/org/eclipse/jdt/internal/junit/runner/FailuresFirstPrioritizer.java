/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import junit.extensions.TestDecorator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FailuresFirstPrioritizer implements ITestPrioritizer {
	private HashSet<String> fPriorities;

	public FailuresFirstPrioritizer(String[] priorities) {
		fPriorities= new HashSet<>(Arrays.asList(priorities));
	}

	@Override
	public Test prioritize(Test suite) {
		doPrioritize(suite, new ArrayList<>());
		return suite;
	}

	private void doPrioritize(Test suite, List<Test> path) {
		if (suite instanceof TestCase) {
			TestCase testCase= (TestCase) suite;
			if (hasPriority(testCase))
				reorder(testCase, path);
		} else if (suite instanceof TestSuite) {
			TestSuite aSuite= (TestSuite)suite;
			path.add(suite);
			loopTests(path, aSuite);
			path.remove(path.size()-1);
		} else if (suite instanceof TestDecorator) {
			TestDecorator aDecorator= (TestDecorator)suite;
			path.add(aDecorator);
			doPrioritize(aDecorator.getTest(), path);
			path.remove(path.size()-1);
		}
	}

	private void loopTests(List<Test> path, TestSuite aSuite) {
		for (Enumeration<Test> e= aSuite.tests(); e.hasMoreElements();) {
			doPrioritize(e.nextElement(), path);
		}
	}


	private void reorder(Test test, List<Test> path) {
		doReorder(test, path, path.size()-1);
	}

	private void doReorder(Test test, List<Test> path, int top) {
		if (top < 0)
			return;
		Test topTest= path.get(top);
		// only reorder TestSuites
		if (topTest instanceof TestSuite) {
			TestSuite suite= (TestSuite) topTest;
			moveTestToFront(suite, test);
		}
		doReorder(topTest, path, top-1);
	}

	void moveTestToFront(TestSuite suite, Test test) {
		@SuppressWarnings("unchecked")
		Vector<Test> tests= (Vector<Test>)getField(suite, "fTests"); //$NON-NLS-1$
		for(int i= 0; i < tests.size(); i++) {
			if (tests.get(i) == test) {
				tests.remove(i);
				tests.insertElementAt(test, 0);
			}
		}
	}


	private boolean hasPriority(TestCase testCase) {
		return fPriorities.contains(testCase.toString());
	}

	public static Object getField(Object object, String fieldName) {
	    return getFieldInClass(object, fieldName, object.getClass());
	}

	private static Object getFieldInClass(Object object, String fieldName, Class<?> clazz) {
		Field field= null;
		if (clazz == null)
			return null;
		try {
			field= clazz.getDeclaredField(fieldName);
	        field.setAccessible(true);
	        return field.get(object);
	    } catch (Exception e) {
	        // fall through
	    }
	    return getFieldInClass(object, fieldName, clazz.getSuperclass());
	}
}
