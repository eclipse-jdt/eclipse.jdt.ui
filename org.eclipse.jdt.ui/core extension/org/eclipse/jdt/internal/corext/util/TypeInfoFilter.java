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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class TypeInfoFilter {
	
	private String fText;
	private IJavaSearchScope fSearchScope;
	private boolean fIsWorkspaceScope;
	private int fElementKind;

	private StringMatcher fPackageMatcher;
	private String fPackagePattern;

	String fNamePattern;
	private StringMatcher fNameMatcher;

	private String fCamelCasePattern;
	private StringMatcher fCamelCaseTailMatcher;
	private StringMatcher fExactNameMatcher;

	private static final char END_SYMBOL= '<';
	private static final char ANY_STRING= '*';
	private static final char BLANK= ' ';
	
	private static final int TYPE_MODIFIERS= Flags.AccEnum | Flags.AccAnnotation | Flags.AccInterface;

	public TypeInfoFilter(String text, IJavaSearchScope scope, int elementKind) {
		fText= text;
		fSearchScope= scope;
		fIsWorkspaceScope= fSearchScope.equals(SearchEngine.createWorkspaceScope());
		fElementKind= elementKind;
		int index= text.lastIndexOf("."); //$NON-NLS-1$
		if (index == -1) {
			createNamePattern(text);
		} else {
			fPackagePattern= text.substring(0, index);
			fPackageMatcher= new StringMatcher(fPackagePattern, true, false);

			createNamePattern(text.substring(index + 1));
		}
		fNameMatcher= new StringMatcher(fNamePattern, true, false);
		if (fCamelCasePattern != null) {
			fExactNameMatcher= new StringMatcher(createTextPatternFromText(fText), true, false);
		}
	}

	public String getText() {
		return fText;
	}

	public boolean isSubFilter(String text) {
		return fText.startsWith(text);
	}

	public boolean isCamcelCasePattern() {
		return fCamelCasePattern != null;
	}

	public String getPackagePattern() {
		return fPackagePattern;
	}

	public String getNamePattern() {
		return fNamePattern;
	}

	public int getSearchFlags() {
		int result= 0;
		if (fCamelCasePattern != null) {
			result= result | SearchPattern.R_CASE_SENSITIVE;
		}
		if (fNamePattern.indexOf("*") != -1) { //$NON-NLS-1$
			result= result | SearchPattern.R_PATTERN_MATCH;
		}
		return result;
	}

	public boolean matchesNameExact(TypeInfo type) {
		return fExactNameMatcher.match(type.getTypeName());
	}

	public boolean matchesSearchResult(TypeInfo type) {
		return matchesCamelCase(type);
	}

	public boolean matchesCachedResult(TypeInfo type) {
		if (!(matchesName(type) && matchesPackage(type)))
			return false;
		return matchesCamelCase(type);
	}
	
	public boolean matchesHistoryElement(TypeInfo type) {
		if (!(matchesName(type) && matchesPackage(type) && matchesModifiers(type) && matchesScope(type)))
			return false;
		return matchesCamelCase(type);
	}

	private boolean matchesCamelCase(TypeInfo type) {
		if (fCamelCasePattern == null)
			return true;
		String name= type.getTypeName();
		int camelCaseIndex= 0;
		int lastUpperCase= Integer.MAX_VALUE;
		for (int i= 0; camelCaseIndex < fCamelCasePattern.length() && i < name.length(); i++) {
			char c= name.charAt(i);
			if (Character.isUpperCase(c)) {
				lastUpperCase= i;
				if (c != fCamelCasePattern.charAt(camelCaseIndex))
					return false;
				camelCaseIndex++;
			}
		}
		if (camelCaseIndex < fCamelCasePattern.length())
			return false;
		// The camel case pattern exactly matches the name
		if (lastUpperCase == name.length() - 1)
			return fCamelCaseTailMatcher == null;
		if (fCamelCaseTailMatcher == null)
			return true;
		return fCamelCaseTailMatcher.match(name.substring(lastUpperCase + 1));
	}

	private boolean matchesName(TypeInfo type) {
		return fNameMatcher.match(type.getTypeName());
	}

	private boolean matchesPackage(TypeInfo type) {
		if (fPackageMatcher == null)
			return true;
		return fPackageMatcher.match(type.getTypeContainerName());
	}

	private void createNamePattern(final String text) {
		int length= text.length();
		fCamelCasePattern= null;
		fNamePattern= text;
		if (length > 0) {
			char c= text.charAt(0);
			if (length > 1 && Character.isUpperCase(c)) {
				StringBuffer patternBuffer= new StringBuffer();
				StringBuffer camelCaseBuffer= new StringBuffer();
				patternBuffer.append(c);
				camelCaseBuffer.append(c);
				int index= 1;
				for (; index < length; index++) {
					c= text.charAt(index);
					if (Character.isUpperCase(c)) {
						patternBuffer.append("*"); //$NON-NLS-1$
						patternBuffer.append(c);
						camelCaseBuffer.append(c);
					} else {
						break;
					}
				}
				if (camelCaseBuffer.length() > 1) {
					if (index == length) {
						fNamePattern= patternBuffer.toString();
						fCamelCasePattern= camelCaseBuffer.toString();
					} else if (restIsLowerCase(text, index)) {
						fNamePattern= patternBuffer.toString();
						fCamelCasePattern= camelCaseBuffer.toString();
						fCamelCaseTailMatcher= new StringMatcher(createTextPatternFromText(text.substring(index)), true, false);
					}
				}
			}
			fNamePattern= createTextPatternFromText(fNamePattern);
		}
	}

	private boolean restIsLowerCase(String s, int start) {
		for (int i= start; i < s.length(); i++) {
			if (Character.isUpperCase(s.charAt(i)))
				return false;
		}
		return true;
	}

	private static String createTextPatternFromText(final String text) {
		int length= text.length();
		String result= text;
		switch (text.charAt(length - 1)) {
			case END_SYMBOL :
				return text.substring(0, length - 1);
			case ANY_STRING :
				return result;
			case BLANK:
				return text.trim();
			default :
				return text + ANY_STRING;
		}
	}
	
	private boolean matchesScope(TypeInfo type) {
		if (fIsWorkspaceScope)
			return true;
		return type.isEnclosed(fSearchScope);
	}
	
	private boolean matchesModifiers(TypeInfo type) {
		if (fElementKind == IJavaSearchConstants.TYPE)
			return true;
		int modifiers= type.getModifiers() & TYPE_MODIFIERS;
		switch (fElementKind) {
			case IJavaSearchConstants.CLASS:
				return modifiers == 0;
			case IJavaSearchConstants.ANNOTATION_TYPE:
				return Flags.isAnnotation(modifiers);
			case IJavaSearchConstants.INTERFACE:
				return Flags.isInterface(modifiers);
			case IJavaSearchConstants.ENUM:
				return Flags.isEnum(modifiers);
			case IJavaSearchConstants.CLASS_AND_INTERFACE:
				return modifiers == 0 || Flags.isInterface(modifiers);
			case IJavaSearchConstants.CLASS_AND_ENUM:
				return modifiers == 0 || Flags.isEnum(modifiers);
		}
		return false;
	}
}