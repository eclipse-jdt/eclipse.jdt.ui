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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.text.JavaWordIterator;

/**
 * This class contains methods for suggesting new names for variables or methods
 * whose name consists at least partly of the name of their declaring type (or
 * in case of methods, the return type or a parameter type).
 * 
 * The methods return the newly suggested method or variable name in case of a
 * match, or null in case nothing matched.
 * 
 * In any case, prefixes and suffixes are removed from variable names. As method
 * names have no configurable suffixes or prefixes, they are left unchanged. The
 * remaining name is called "stripped element name".
 * 
 * After the match according to the strategy, prefixes and suffixes are
 * reapplied to the names.
 * 
 * EXACT STRATEGY (always performed).
 * ----------------------------------------------------------------
 * 
 * The stripped element name is directly compared with the type name:
 * 
 * a) the first character must match case-insensitive
 * 
 * b) all other characters must match case-sensitive
 * 
 * In case of a match, the new type name is returned (first character adapted,
 * respectively). Suffixes/Prefixes are reapplied.
 * 
 * Note that this also matches fields with names like "SomeField", "fsomeField",
 * and method names like "JavaElement()".
 * 
 * EMBEDDED STRATEGY (performed second if chosen by user).
 * ----------------------------------------------------------------
 * 
 * A search is performed in the stripped element name for the old type name:
 * 
 * a) the first character must match case-insensitive
 * 
 * b) all other characters must match case-sensitive
 * 
 * c) the stripped element name must end after the type name, or the next
 * character must be a letter, or the next character must be upper cased.
 * 
 * In case of a match, the new type is inserted into the stripped element name,
 * replacing the old type name, first character adapted to the correct case.
 * Suffixes/Prefixes are reapplied.
 * 
 * Note that this also matches methods with names like "createjavaElement()" or
 * fields like "fjavaElementCache".
 * 
 * SUFFIX STRATEGY (performed third if chosen by user)
 * ----------------------------------------------------------------
 * 
 * The new and old type names are analyzed for "camel case suffixes", that is,
 * substrings which begin with an uppercased letter. For example,
 * "SimpleJavaElement" is split into the three suffixes "SimpleJavaElement",
 * "JavaElement", and "Element". If one type name has more suffixes than the
 * other, both are stripped to the smaller size.
 * 
 * Then, a search is performed in the stripped variable name for the suffixes of
 * the old type from larger to smaller suffixes. Each suffix must match like in
 * the embedded strategy, i.e.
 * 
 * a) the first character must match case-insensitive
 * 
 * b) all other characters must match case-sensitive
 * 
 * c) the stripped element name must end after the suffix, or the next character
 * must be a letter, or the next character must be upper cased.
 * 
 * In case of a match, the corresponding suffix of the new type is inserted at
 * the position of the old suffix. Suffixes/Prefixes are reapplied.
 * 
 * Note that numbers and other non-letter characters belong to the previous
 * camel case substring. Note also that this matches names like "cachedelement"
 * for type "IJavaElement" and also "myE" for the type "LetterE". It does not
 * match "myClass" for type "A".
 * 
 * 
 * @since 3.2
 * 
 */
public class RenamingNameSuggestor {

	public static final int STRATEGY_EXACT= 1;
	public static final int STRATEGY_EMBEDDED= 2;
	public static final int STRATEGY_SUFFIX= 3;

	private int fStrategy;
	private boolean fSkipLeadingI;
	private String[] fFieldPrefixes;
	private String[] fFieldSuffixes;
	private String[] fStaticFieldPrefixes;
	private String[] fStaticFieldSuffixes;

	public RenamingNameSuggestor() {
		this(STRATEGY_SUFFIX);
	}

	public RenamingNameSuggestor(int strategy) {

		Assert.isTrue(strategy >= 1 && strategy <= 3);

		fStrategy= strategy;
		fSkipLeadingI= true;

		resetPrefixes();
	}

	public String suggestNewFieldName(IJavaProject project, String oldFieldName, boolean isStatic, String oldTypeName, String newTypeName) {

		initializePrefixesAndSuffixes(project);

		if (isStatic)
			return suggestNewVariableName(fStaticFieldPrefixes, fStaticFieldSuffixes, oldFieldName, oldTypeName, newTypeName);
		else
			return suggestNewVariableName(fFieldPrefixes, fFieldSuffixes, oldFieldName, oldTypeName, newTypeName);
	}

	public String suggestNewMethodName(String oldMethodName, String oldTypeName, String newTypeName) {

		Assert.isNotNull(oldMethodName);
		Assert.isNotNull(oldTypeName);
		Assert.isNotNull(newTypeName);
		Assert.isTrue(oldMethodName.length() > 0);
		Assert.isTrue(oldTypeName.length() > 0);
		Assert.isTrue(newTypeName.length() > 0);

		resetPrefixes();

		return match(oldTypeName, newTypeName, oldMethodName);
	}

	public String suggestNewVariableName(String[] prefixes, String[] suffixes, String oldVariableName, String oldTypeName, String newTypeName) {

		Assert.isNotNull(prefixes);
		Assert.isNotNull(suffixes);
		Assert.isNotNull(oldVariableName);
		Assert.isNotNull(oldTypeName);
		Assert.isNotNull(newTypeName);
		Assert.isTrue(oldVariableName.length() > 0);
		Assert.isTrue(oldTypeName.length() > 0);
		Assert.isTrue(newTypeName.length() > 0);

		final String usedPrefix= findLongestPrefix(oldVariableName, prefixes);
		final String usedSuffix= findLongestSuffix(oldVariableName, suffixes);
		final String strippedVariableName= oldVariableName.substring(usedPrefix.length(), oldVariableName.length() - usedSuffix.length());

		String newVariableName= match(oldTypeName, newTypeName, strippedVariableName);
		return (newVariableName != null) ? usedPrefix + newVariableName + usedSuffix : null;
	}

	// -------------------------------------- Match methods

	private String match(final String oldTypeName, final String newTypeName, final String strippedVariableName) {

		String oldType= oldTypeName;
		String newType= newTypeName;

		if (fSkipLeadingI && isInterfaceName(oldType) && isInterfaceName(newType)) {
			oldType= getInterfaceName(oldType);
			newType= getInterfaceName(newType);
		}

		String newVariableName= null;

		/*
		 * Use all strategies applied by the user. Always start with exact
		 * matching.
		 * 
		 * Note that suffix matching may not match the whole type name if the
		 * new type name has a smaller camel case chunk count.
		 */

		newVariableName= exactMatch(oldType, newType, strippedVariableName);
		if (newVariableName == null && fStrategy >= STRATEGY_EMBEDDED)
			newVariableName= embeddedMatch(oldType, newType, strippedVariableName);
		if (newVariableName == null && fStrategy >= STRATEGY_SUFFIX)
			newVariableName= suffixMatch(oldType, newType, strippedVariableName);

		return newVariableName;
	}

	private String exactMatch(final String oldTypeName, final String newTypeName, final String strippedVariableName) {

		if (strippedVariableName.equals(oldTypeName))
			return newTypeName;

		if (strippedVariableName.equals(getLowerCased(oldTypeName)))
			return getLowerCased(newTypeName);

		return null;
	}

	private String embeddedMatch(String oldTypeName, String newTypeName, String strippedVariableName) {

		String newName= embeddedDirectMatch(oldTypeName, newTypeName, strippedVariableName);
		if (newName != null)
			return newName;

		return embeddedDirectMatch(getLowerCased(oldTypeName), getLowerCased(newTypeName), strippedVariableName);
	}

	private String suffixMatch(final String oldType, final String newType, final String strippedVariableName) {

		// get an array of all camel-cased elements from both types
		String[] suffixesOld= getSuffixes(oldType);
		String[] suffixesNew= getSuffixes(newType);

		// get an equal-sized array of the last n camel-cased elements
		int min= Math.min(suffixesOld.length, suffixesNew.length);
		String[] suffixesOldEqual= new String[min];
		String[] suffixesNewEqual= new String[min];
		System.arraycopy(suffixesOld, suffixesOld.length - min, suffixesOldEqual, 0, min);
		System.arraycopy(suffixesNew, suffixesNew.length - min, suffixesNewEqual, 0, min);

		// match 'em
		for (int i= 0; i < suffixesOldEqual.length; i++) {
			String oldCamelCaseMatch= concatToEndOfArray(suffixesOldEqual, i);
			String newCamelCaseMatch= concatToEndOfArray(suffixesNewEqual, i);
			String newName= embeddedDirectMatch(oldCamelCaseMatch, newCamelCaseMatch, strippedVariableName);

			if (newName != null)
				return newName;

			// try lowercase, for example for field "element"
			newName= embeddedDirectMatch(getLowerCased(oldCamelCaseMatch), getLowerCased(newCamelCaseMatch), strippedVariableName);

			if (newName != null)
				return newName;
		}

		return null;
	}


	// ---------------- Helper methods


	private String embeddedDirectMatch(String oldTypeName, String newTypeName, String strippedVariableName) {

		final int index= strippedVariableName.indexOf(oldTypeName);

		if (index != -1) {
			final String prefix= strippedVariableName.substring(0, index);
			final String suffix= strippedVariableName.substring(index + oldTypeName.length());

			if (suffix.length() == 0)
				return prefix + newTypeName;
			else if (Character.isDigit(suffix.charAt(0)) || Character.isUpperCase(suffix.charAt(0)))
				return prefix + newTypeName + suffix;
		}

		return null;
	}

	/**
	 * Grab a list of camelCase-separated suffixes from the typeName, for
	 * example:
	 * 
	 * "JavaElementName" => { "Java", "Element", "Name }
	 * 
	 * "ASTNode" => { "AST", "Node" }
	 * 
	 */
	private String[] getSuffixes(String typeName) {
		List suffixes= new ArrayList();
		JavaWordIterator iterator= new JavaWordIterator();
		iterator.setText(typeName);
		int lastmatch= 0;
		int match;
		while ( (match= iterator.next()) != BreakIterator.DONE) {
			suffixes.add(typeName.substring(lastmatch, match));
			lastmatch= match;
		}
		return (String[]) suffixes.toArray(new String[0]);
	}

	private String concatToEndOfArray(String[] suffixesNewEqual, int i) {
		StringBuffer returner= new StringBuffer();
		for (int j= i; j < suffixesNewEqual.length; j++) {
			returner.append(suffixesNewEqual[j]);
		}
		return returner.toString();
	}

	private String getLowerCased(String name) {
		if (name.length() > 1)
			return Character.toLowerCase(name.charAt(0)) + name.substring(1);
		else
			return name.toLowerCase();
	}

	private boolean isInterfaceName(String typeName) {
		return ( (typeName.length() >= 2) && typeName.charAt(0) == 'I' && Character.isUpperCase(typeName.charAt(1)));
	}

	private String getInterfaceName(String typeName) {
		return typeName.substring(1);
	}

	private String findLongestPrefix(String name, String[] prefixes) {
		String usedPrefix= ""; //$NON-NLS-1$
		int bestLen= 0;
		for (int i= 0; i < prefixes.length; i++) {
			if (name.startsWith(prefixes[i])) {
				if (prefixes[i].length() > bestLen) {
					bestLen= prefixes[i].length();
					usedPrefix= prefixes[i];
				}
			}
		}
		return usedPrefix;
	}

	private String findLongestSuffix(String name, String[] suffixes) {
		String usedPrefix= ""; //$NON-NLS-1$
		int bestLen= 0;
		for (int i= 0; i < suffixes.length; i++) {
			if (name.endsWith(suffixes[i])) {
				if (suffixes[i].length() > bestLen) {
					bestLen= suffixes[i].length();
					usedPrefix= suffixes[i];
				}
			}
		}
		return usedPrefix;
	}

	private void resetPrefixes() {
		fFieldPrefixes= new String[0];
		fFieldSuffixes= new String[0];
		fStaticFieldPrefixes= new String[0];
		fStaticFieldSuffixes= new String[0];
	}

	private void initializePrefixesAndSuffixes(IJavaProject project) {

		Assert.isNotNull(project);

		String fieldPrefixes= project.getOption(JavaCore.CODEASSIST_FIELD_PREFIXES, true);
		if (fieldPrefixes != null)
			fFieldPrefixes= fieldPrefixes.split(","); //$NON-NLS-1$
		else
			fFieldPrefixes= new String[0];

		String fieldSuffixes= project.getOption(JavaCore.CODEASSIST_FIELD_SUFFIXES, true);
		if (fieldSuffixes != null)
			fFieldSuffixes= fieldSuffixes.split(","); //$NON-NLS-1$
		else
			fFieldSuffixes= new String[0];

		String staticFieldPrefixes= project.getOption(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, true);
		if (staticFieldPrefixes != null)
			fStaticFieldPrefixes= staticFieldPrefixes.split(","); //$NON-NLS-1$
		else
			fStaticFieldPrefixes= new String[0];

		String staticFieldSuffixes= project.getOption(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, true);
		if (staticFieldSuffixes != null)
			fStaticFieldSuffixes= staticFieldSuffixes.split(","); //$NON-NLS-1$
		else
			fStaticFieldPrefixes= new String[0];
	}

}
