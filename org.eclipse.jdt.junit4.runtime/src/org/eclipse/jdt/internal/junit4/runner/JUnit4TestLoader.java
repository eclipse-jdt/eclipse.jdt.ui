/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *     Andrew Eisenberg <andrew@eisenberg.as> - [JUnit] Rerun failed first does not work with JUnit4 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=140392
 *******************************************************************************/
package org.eclipse.jdt.internal.junit4.runner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;

import org.eclipse.jdt.internal.junit.runner.ITestLoader;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader;
import org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestReference;

import junit.framework.Test;

public class JUnit4TestLoader implements ITestLoader {

	@Override
	public ITestReference[] loadTests(
			Class<?>[] testClasses,
			String testName,
			String[] failureNames,
			String[] packages,
			String[][] includeExcludeTags,
			String uniqueId,
			RemoteTestRunner listener) {

		ITestReference[] refs= new ITestReference[testClasses.length];
		for (int i= 0; i < testClasses.length; i++) {
			Class<?> clazz= testClasses[i];
			ITestReference ref= createTest(clazz, testName, failureNames, listener);
			refs[i]= ref;
		}
		return refs;
	}

	private Description getRootDescription(Runner runner, DescriptionMatcher matcher) {
		Description current= runner.getDescription();
		while (true) {
			List<Description> children= current.getChildren();
			if (children.size() != 1 || matcher.matches(current))
				return current;
			current= children.get(0);
		}
	}

	private ITestReference createTest(Class<?> clazz, String testName, String[] failureNames, RemoteTestRunner listener) {
		if (clazz == null)
			return null;
		if (testName != null && isJUnit3SetUpTest(clazz, testName)) {
			JUnit3TestLoader jUnit3TestLoader= new JUnit3TestLoader();
			Test test= jUnit3TestLoader.getTest(clazz, testName, listener);
			return new JUnit3TestReference(test);
		}
		if (testName != null) {
			return createFilteredTest(clazz, testName, failureNames);
		}
		return createUnfilteredTest(clazz, failureNames);
	}

	private ITestReference createFilteredTest(Class<?> clazz, String testName, String[] failureNames) {
		DescriptionMatcher matcher= DescriptionMatcher.create(clazz, testName);
		SubForestFilter filter= new SubForestFilter(matcher);
		Request request= sortByFailures(Request.classWithoutSuiteMethod(clazz).filterWith(filter), failureNames);
		Runner runner= request.getRunner();
		Description description= getRootDescription(runner, matcher);
		return new JUnit4TestReference(runner, description);
	}

	private ITestReference createUnfilteredTest(Class<?> clazz, String[] failureNames) {
		Request request= sortByFailures(Request.aClass(clazz), failureNames);
		Runner runner= request.getRunner();
		Description description= runner.getDescription();
		return new JUnit4TestReference(runner, description);
	}

	private Request sortByFailures(Request request, String[] failureNames) {
		if (failureNames != null) {
			return request.sortWith(new FailuresFirstSorter(failureNames));
		}
		return request;
	}

	private boolean isJUnit3SetUpTest(Class<?> clazz, String testName) {
		if (!Test.class.isAssignableFrom(clazz))
			return false;
		try {
			Method testMethod= clazz.getMethod(testName);
			if (testMethod.getAnnotation(org.junit.Test.class) != null)
				return false;

			Method setup= clazz.getMethod(JUnit3TestLoader.SET_UP_TEST_METHOD_NAME, Test.class);
			int modifiers= setup.getModifiers();
			if (setup.getReturnType() == Test.class && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))
				return true;
		} catch (SecurityException e1) {
		} catch (NoSuchMethodException e) {
		}
		return false;
	}
}
