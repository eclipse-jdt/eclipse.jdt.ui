/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;

import org.eclipse.jdt.internal.ui.util.PatternMatcher;

public class TypeInfoFilter {

	private final String fText;

	private final IJavaSearchScope fSearchScope;

	private final boolean fIsWorkspaceScope;

	private final int fElementKind;

	private final ITypeInfoFilterExtension fFilterExtension;

	private final TypeInfoRequestorAdapter fAdapter= new TypeInfoRequestorAdapter();

	private final PatternMatcher fPackageMatcher;

	private final PatternMatcher fNameMatcher;

	private static final int TYPE_MODIFIERS= Flags.AccEnum | Flags.AccAnnotation | Flags.AccInterface;

	/* reduces filenames and stack traces to class name */
	public static String simplifySearchText(String input) {
		String s= input;
		try {
			int i;
			// skip any bracket and anything behind ("[native]" from thread dump)
			i= s.lastIndexOf('[');
			if (i != -1) {
				s= s.substring(0, i);
			}
			// skip any bracket, anything behind and the segment before (method name)
			i= s.lastIndexOf('(');
			if (i != -1) {
				s= s.substring(0, i);
				i= s.lastIndexOf('.');
				if (i != -1) {
					s= s.substring(0, i);
				}
			}
			// skip any "$$Lambda" and anything behind (Lambda)
			i= s.lastIndexOf("$$Lambda"); //$NON-NLS-1$
			if (i != -1) {
				s= s.substring(0, i);
			}
			// skip any backslash and the text before (windows path)
			s= skipBefore(s, "\\"); //$NON-NLS-1$
			// skip any slash and the text before (unix path)
			s= skipBefore(s, "/"); //$NON-NLS-1$
			// skip initial "at " and the text before (StackTraceElement)
			s= skipBefore(s, "at "); //$NON-NLS-1$
			// simplify
			s= s.strip();
			// skip last single ".java" and anything behind (file name)
			s= skipEnding(s, ".java"); //$NON-NLS-1$
			s= skipEnding(s, ".class"); //$NON-NLS-1$
			// skip $1, $2 ... - typenames cannot start with a number
			s= skipSynthetic(s);
			// binary name of Inner Class to qualified name
			int inner= countContains(s, '$');
			if (inner > 0) {
				s= s.replace('$', '.');
			}
			if (s.isEmpty()) {
				// oversimplified
				return input;
			}
			// partially qualified names need a asterisk wildcard to be found by Open Type dialog:
			if (inner > 0 && !input.trim().equals(s.trim()) && countContains(s, '.') == inner && !s.contains("*")) { //$NON-NLS-1$
				s= "*." + s; //$NON-NLS-1$
			}
		} catch (Exception e) {
			// just in case anything bad happened:
			return input;
		}
		return s;
	}

	private static int countContains(String s, char c) {
		return (int) s.chars().filter(i -> c == (char) i).count();
	}

	private static String skipBefore(String s, String skip) {
		int i= s.lastIndexOf(skip);
		if (i != -1) {
			s= s.substring(i + skip.length());
		}
		return s;
	}

	private static String skipEnding(String s, String skip) {
		int i= s.lastIndexOf(skip);
		int length= skip.length();
		char nextChar= s.length() > i + length ? s.charAt(i + length) : ' ';
		if (i != -1 && !Character.isJavaIdentifierPart(nextChar) && !(nextChar == '.')) {
			s= s.substring(0, i);
		}
		return s;
	}
	private static String skipSynthetic(String s) {
		int i= s.lastIndexOf('$');
		int length= 1;
		char nextChar= s.length() > i + length ? s.charAt(i + length) : ' ';
		if (i != -1 && !Character.isJavaIdentifierStart(nextChar) && !(nextChar == '.')) {
			s= s.substring(0, i);
		}
		return s;
	}

	public TypeInfoFilter(String text, IJavaSearchScope scope, int elementKind, ITypeInfoFilterExtension extension) {
		fText= text;
		fSearchScope= scope;
		fIsWorkspaceScope= fSearchScope.equals(SearchEngine.createWorkspaceScope());
		fElementKind= elementKind;
		fFilterExtension= extension;

		int index= text.lastIndexOf("."); //$NON-NLS-1$
		if (index == -1) {
			fNameMatcher= new PatternMatcher(text);
			fPackageMatcher= null;
		} else {
			if (Character.isUpperCase(text.charAt(0))) {
				// might be referring to class so add wild-card at front
				fPackageMatcher= new PatternMatcher(evaluatePackagePattern("*" + text.substring(0, index))); //$NON-NLS-1$
			} else {
				fPackageMatcher= new PatternMatcher(evaluatePackagePattern(text.substring(0, index)));
			}
			String name= text.substring(index + 1);
			if (name.length() == 0)
				name= "*"; //$NON-NLS-1$
			fNameMatcher= new PatternMatcher(name);
		}
	}

	/*
	 * Transforms o.e.j  to o*.e*.j*
	 */
	private String evaluatePackagePattern(String s) {
		StringBuilder buf= new StringBuilder();
		boolean hasWildCard= false;
		int len= s.length();
		for (int i= 0; i < len; i++) {
			char ch= s.charAt(i);
			if (ch == '.') {
				if (!hasWildCard) {
					buf.append('*');
				}
				hasWildCard= false;
			} else if (ch == '*' || ch == '?') {
				hasWildCard= true;
			}
			buf.append(ch);
		}
		if (!hasWildCard) {
			if (len == 0) {
				buf.append('?');
			}
			buf.append('*');
		}
		return buf.toString();
	}

	public String getText() {
		return fText;
	}

	/**
	 * Checks whether <code>this</code> filter is a subFilter of the given <code>text</code>.
	 * <p>
	 * <i>WARNING: This is the <b>reverse</b> interpretation compared to
	 * {@link org.eclipse.ui.dialogs.SearchPattern#isSubPattern(org.eclipse.ui.dialogs.SearchPattern)}
	 * and {@link org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isSubFilter}. </i>
	 * </p>
	 *
	 * @param text another filter text
	 * @return <code>true</code> if <code>this</code> filter is a subFilter of <code>text</code>
	 *         e.g. "List" is a subFilter of "L". In this case, the filters matches a proper subset
	 *         of the items matched by <code>text</code>.
	 */
	public boolean isSubFilter(String text) {
		if (!fText.startsWith(text))
			return false;

		return fText.indexOf('.', text.length()) == -1;
	}

	public boolean isCamelCasePattern() {
		int ccMask= SearchPattern.R_CAMELCASE_MATCH | SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH;
		return (fNameMatcher.getMatchKind() & ccMask) != 0;
	}

	public String getPackagePattern() {
		if (fPackageMatcher == null)
			return null;
		return fPackageMatcher.getPattern();
	}

	public String getNamePattern() {
		return fNameMatcher.getPattern();
	}

	public int getSearchFlags() {
		return fNameMatcher.getMatchKind();
	}

	public int getElementKind() {
		return fElementKind;
	}

	public IJavaSearchScope getSearchScope() {
		return fSearchScope;
	}

	public int getPackageFlags() {
		if (fPackageMatcher == null)
			return SearchPattern.R_EXACT_MATCH;

		return fPackageMatcher.getMatchKind();
	}

	public boolean matchesRawNamePattern(TypeNameMatch type) {
		return Strings.startsWithIgnoreCase(type.getSimpleTypeName(), fNameMatcher.getPattern());
	}

	public boolean matchesCachedResult(TypeNameMatch type) {
		if (!matchesPackage(type) || !matchesFilterExtension(type))
			return false;
		return matchesName(type);
	}

	public boolean matchesHistoryElement(TypeNameMatch type) {
		if (!matchesPackage(type)
				|| !matchesModifiers(type)
				|| !matchesScope(type)
				|| !matchesFilterExtension(type))
			return false;
		return matchesName(type);
	}

	public boolean matchesFilterExtension(TypeNameMatch type) {
		if (fFilterExtension == null)
			return true;
		fAdapter.setMatch(type);
		return fFilterExtension.select(fAdapter);
	}

	private boolean matchesName(TypeNameMatch type) {
		if (fText.length() == 0) {
			return true; //empty pattern matches all names
		}
		return fNameMatcher.matches(type.getSimpleTypeName());
	}

	private boolean matchesPackage(TypeNameMatch type) {
		if (fPackageMatcher == null)
			return true;
		return fPackageMatcher.matches(type.getTypeContainerName());
	}

	private boolean matchesScope(TypeNameMatch type) {
		if (fIsWorkspaceScope)
			return true;
		return fSearchScope.encloses(type.getType());
	}

	private boolean matchesModifiers(TypeNameMatch type) {
		if (fElementKind == IJavaSearchConstants.TYPE)
			return true;
		int modifiers= type.getModifiers() & TYPE_MODIFIERS;
		switch (fElementKind) {
			case IJavaSearchConstants.CLASS:
				return modifiers == 0;
			case IJavaSearchConstants.ANNOTATION_TYPE:
				return Flags.isAnnotation(modifiers);
			case IJavaSearchConstants.INTERFACE:
				return modifiers == Flags.AccInterface;
			case IJavaSearchConstants.ENUM:
				return Flags.isEnum(modifiers);
			case IJavaSearchConstants.CLASS_AND_INTERFACE:
				return modifiers == 0 || modifiers == Flags.AccInterface;
			case IJavaSearchConstants.CLASS_AND_ENUM:
				return modifiers == 0 || Flags.isEnum(modifiers);
			case IJavaSearchConstants.INTERFACE_AND_ANNOTATION:
				return Flags.isInterface(modifiers);
		}
		return false;
	}
}
