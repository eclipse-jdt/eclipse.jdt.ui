/*******************************************************************************
 * Copyright (c) 2014, 2021 Moritz Eysholdt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Moritz Eysholdt <moritz.eysholdt@itemis.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit4.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.Description;
import org.junit.runners.Parameterized;

/**
 * This class matches JUnit's {@link Description} against a string.
 *
 * See {@link #create(Class, String)} for details.
 */
public abstract class DescriptionMatcher {

	private static class CompositeMatcher extends DescriptionMatcher {
		private final List<DescriptionMatcher> fMatchers;

		public CompositeMatcher(List<DescriptionMatcher> matchers) {
			fMatchers= matchers;
		}

		@Override
		public boolean matches(Description description) {
			for (DescriptionMatcher matcher : fMatchers)
				if (matcher.matches(description))
					return true;
			return false;
		}

		@Override
		public String toString() {
			return fMatchers.toString();
		}
	}

	private static class ExactMatcher extends DescriptionMatcher {
		private final String fDisplayName;

		public ExactMatcher(String className) {
			fDisplayName= className;
		}

		public ExactMatcher(String className, String testName) {
			// see org.junit.runner.Description.formatDisplayName(String, String)
			fDisplayName= String.format("%s(%s)", testName, className); //$NON-NLS-1$
		}

		@Override
		public boolean matches(Description description) {
			return fDisplayName.equals(description.getDisplayName());
		}

		@Override
		public String toString() {
			return String.format("{%s:fDisplayName=%s]", getClass().getSimpleName(), fDisplayName); //$NON-NLS-1$
		}
	}

	/**
	 * This class extracts the leading chars from {@link Description#getMethodName()} which are a
	 * valid Java identifier. If this identifier equals this class' identifier, the Description is
	 * matched.
	 *
	 * Please be aware that {@link Description#getMethodName()} can be any value a JUnit runner has
	 * computed. It is not necessarily a valid method name. For example, {@link Parameterized} uses
	 * the format 'methodname[i]', with 'i' being the row index in the table of test data.
	 */
	private static class LeadingIdentifierMatcher extends DescriptionMatcher {

		private final String fClassName;

		private final String fLeadingIdentifier;

		public LeadingIdentifierMatcher(String className, String leadingIdentifier) {
			super();
			fClassName= className;
			fLeadingIdentifier= leadingIdentifier;
		}

		@Override
		public boolean matches(Description description) {
			String className= description.getClassName();
			if (fClassName.equals(className)) {
				String methodName= description.getMethodName();
				if (methodName != null) {
					return fLeadingIdentifier.equals(extractLeadingIdentifier(methodName));
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return String.format("{%s:fClassName=%s,fLeadingIdentifier=%s]", getClass().getSimpleName(), fClassName, fLeadingIdentifier); //$NON-NLS-1$
		}

	}

	/**
	 * Creates a matcher object that can decide for {@link Description}s whether they match the
	 * supplied 'matchString' or not.
	 *
	 * Several strategies for matching are applied:
	 * <ul>
	 * <li>if 'matchString' equals {@link Description#getDisplayName()}, it's always a match</li>
	 * <li>if 'matchString' has the format foo(bar), which is JUnit's format, it tries to match
	 * methods base on leading identifiers. See {@link LeadingIdentifierMatcher}.</li>
	 * <li>if 'matchString' does not have the format foo(bar), it also tries to match Descriptions
	 * that equal clazz(matchString), with 'clazz' being this method's parameter. Furthermore, if
	 * 'matchString' is a Java identifier, it matches Descriptions via leading identifiers. See
	 * {@link LeadingIdentifierMatcher}</li>
	 * </ul>
	 *
	 * @param clazz A class that is used when 'matchString' does not have the format
	 *            methodName(className).
	 *
	 * @param matchString A string to match JUnit's {@link Description}s against.
	 *
	 * @return A matcher object.
	 */
	public static DescriptionMatcher create(Class<?> clazz, String matchString) {
		String className= clazz.getName();
		List<DescriptionMatcher> matchers= new ArrayList<>();
		matchers.add(new ExactMatcher(matchString));
		Matcher parsed= METHOD_AND_CLASS_NAME_PATTERN.matcher(matchString);
		if (parsed.matches()) {
			String testName= parsed.group(1);
			if (testName.equals(extractLeadingIdentifier(testName)))
				matchers.add(new LeadingIdentifierMatcher(className, testName));
		} else {
			if (!className.equals(matchString)) {
				matchers.add(new ExactMatcher(className, matchString));
				if (matchString.equals(extractLeadingIdentifier(matchString)))
					matchers.add(new LeadingIdentifierMatcher(className, matchString));
			}
		}
		return new CompositeMatcher(matchers);
	}

	private static String extractLeadingIdentifier(String string) {
		if (string.length() == 0)
			return null;
		if (!Character.isJavaIdentifierStart(string.charAt(0)))
			return null;
		for (int i= 1; i < string.length(); i++) {
			if (!Character.isJavaIdentifierPart(string.charAt(i))) {
				return string.substring(0, i);
			}
		}
		return string;
	}

	// see org.junit.runner.Description.METHOD_AND_CLASS_NAME_PATTERN
	private static final Pattern METHOD_AND_CLASS_NAME_PATTERN= Pattern.compile("(.*)\\((.*)\\)"); //$NON-NLS-1$

	public abstract boolean matches(Description description);
}
