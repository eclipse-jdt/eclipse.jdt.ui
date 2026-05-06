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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;

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

	@Override
	public ITestReference[] loadTests(
			LinkedHashMap<Class<?>, List<String>> classToMethods,
			String[] failureNames,
			String[] packages,
			String[][] includeExcludeTags,
			String uniqueId,
			RemoteTestRunner listener) {

		if (classToMethods == null || classToMethods.isEmpty()) {
			return new ITestReference[0];
		}
		// JUnit 4 has no native multi-method selector, but a single class can be
		// filtered down to an arbitrary set of methods via Request#filterWith. We
		// therefore create one ITestReference per class (each running in the same JVM)
		// instead of one per (class, method) pair.
		List<ITestReference> refs= new ArrayList<>(classToMethods.size());
		for (Map.Entry<Class<?>, List<String>> entry : classToMethods.entrySet()) {
			Class<?> clazz= entry.getKey();
			if (clazz == null) {
				continue;
			}
			List<String> methods= entry.getValue();
			if (methods == null || methods.isEmpty()) {
				ITestReference unfiltered= createUnfilteredTest(clazz, failureNames);
				if (unfiltered != null) {
					refs.add(unfiltered);
				}
				continue;
			}
			ITestReference ref= createMultiMethodTest(clazz, methods, failureNames, listener);
			if (ref != null) {
				refs.add(ref);
			}
		}
		return refs.toArray(new ITestReference[0]);
	}

	private ITestReference createMultiMethodTest(Class<?> clazz, List<String> methodNames, String[] failureNames, RemoteTestRunner listener) {
		// Fast path: a single method behaves exactly like the legacy -test path so we
		// preserve identical behavior (incl. JUnit 3 setUpTest detection).
		if (methodNames.size() == 1) {
			return createTest(clazz, methodNames.get(0), failureNames, listener);
		}
		Set<String> distinct= new HashSet<>(methodNames);
		Request request= sortByFailures(Request.classWithoutSuiteMethod(clazz).filterWith(new MultiMethodFilter(distinct)), failureNames);
		Runner runner= request.getRunner();
		Description description= runner.getDescription();
		return new JUnit4TestReference(runner, description);
	}

	/**
	 * Filter that accepts any test {@link Description} whose method name (with the
	 * trailing parameter list, if present, stripped) matches one of the requested
	 * method names. Suites and parent descriptions are accepted when at least one
	 * descendant matches.
	 */
	private static final class MultiMethodFilter extends Filter {
		private final Set<String> fMethodNames;

		MultiMethodFilter(Set<String> methodNames) {
			fMethodNames= methodNames;
		}

		@Override
		public boolean shouldRun(Description description) {
			if (description.isTest()) {
				String name= description.getMethodName();
				if (name == null) {
					return false;
				}
				int paren= name.indexOf('(');
				if (paren > 0) {
					name= name.substring(0, paren);
				}
				int bracket= name.indexOf('[');
				if (bracket > 0) {
					// strip parameterized invocation index, e.g. "myTest[0]" -> "myTest"
					name= name.substring(0, bracket);
				}
				return fMethodNames.contains(name);
			}
			for (Description child : description.getChildren()) {
				if (shouldRun(child)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String describe() {
			return "matches any of " + fMethodNames; //$NON-NLS-1$
		}
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
			// ignore
		} catch (NoSuchMethodException e) {
			// ignore
		}
		return false;
	}
}
