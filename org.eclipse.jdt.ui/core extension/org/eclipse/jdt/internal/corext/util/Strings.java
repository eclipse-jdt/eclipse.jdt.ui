/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.util.Objects;

import org.eclipse.osgi.util.TextProcessor;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

/**
 * Helper class to provide String manipulation functions not available in standard JDK.
 *
 * @see JDTUIHelperClasses
 */
public class Strings extends org.eclipse.jdt.internal.core.manipulation.util.Strings {

	private Strings(){}

	/**
	 * Sets the given <code>styler</code> to use for <code>matchingRegions</code> (obtained from
	 * {@link org.eclipse.jdt.core.search.SearchPattern#getMatchingRegions}) in the
	 * <code>styledString</code> starting from the given <code>index</code>.
	 *
	 * @param styledString the styled string to mark
	 * @param index the index from which to start marking
	 * @param matchingRegions the regions to mark
	 * @param styler the styler to use for marking
	 */
	public static void markMatchingRegions(StyledString styledString, int index, int[] matchingRegions, Styler styler) {
		if (matchingRegions != null) {
			int offset= -1;
			int length= 0;
			for (int i= 0; i + 1 < matchingRegions.length; i= i + 2) {
				if (offset == -1)
					offset= index + matchingRegions[i];

				// Concatenate adjacent regions
				if (i + 2 < matchingRegions.length && matchingRegions[i] + matchingRegions[i + 1] == matchingRegions[i + 2]) {
					length= length + matchingRegions[i + 1];
				} else {
					styledString.setStyle(offset, length + matchingRegions[i + 1], styler);
					offset= -1;
					length= 0;
				}
			}
		}
	}

	/**
	 * Adds special marks so that that the given styled string is readable in a BiDi environment.
	 *
	 * @param styledString the styled string
	 * @return the processed styled string
	 * @since 3.4
	 */
	public static StyledString markLTR(StyledString styledString) {

		/*
		 * NOTE: For performance reasons we do not call  markLTR(styledString, null)
		 */

		if (!USE_TEXT_PROCESSOR)
			return styledString;

		String inputString= styledString.getString();
		String string= TextProcessor.process(inputString);
		if (!Objects.equals(string, inputString))
			insertMarks(styledString, inputString, string);
		return styledString;
	}

	/**
	 * Adds special marks so that that the given styled Java element label is readable in a BiDi
	 * environment.
	 *
	 * @param styledString the styled string
	 * @return the processed styled string
	 * @since 3.6
	 */
	public static StyledString markJavaElementLabelLTR(StyledString styledString) {
		if (!USE_TEXT_PROCESSOR)
			return styledString;

		String inputString= styledString.getString();
		String string= TextProcessor.process(inputString, JAVA_ELEMENT_DELIMITERS);
		if (!Objects.equals(string, inputString))
			insertMarks(styledString, inputString, string);
		return styledString;
	}

	/**
	 * Adds special marks so that that the given styled string is readable in a BiDi environment.
	 *
	 * @param styledString the styled string
	 * @param delimiters the additional delimiters
	 * @return the processed styled string
	 * @since 3.4
	 */
	public static StyledString markLTR(StyledString styledString, String delimiters) {
		if (!USE_TEXT_PROCESSOR)
			return styledString;

		String inputString= styledString.getString();
		String string= TextProcessor.process(inputString, delimiters);
		if (!Objects.equals(string, inputString))
			insertMarks(styledString, inputString, string);
		return styledString;
	}

	/**
	 * Inserts the marks into the given styled string.
	 *
	 * @param styledString the styled string
	 * @param originalString the original string
	 * @param processedString the processed string
	 * @since 3.5
	 */
	private static void insertMarks(StyledString styledString, String originalString, String processedString) {
		int originalLength= originalString.length();
		int processedStringLength= processedString.length();
		char orig= originalLength > 0 ? originalString.charAt(0) : '\0';
		for (int o= 0, p= 0; p < processedStringLength; p++) {
			char processed= processedString.charAt(p);
			if (o < originalLength) {
				if (orig == processed) {
					o++;
					if (o < originalLength)
						orig= originalString.charAt(o);
					continue;
				}
			}
			styledString.insert(processed, p);
		}
	}

	public static String removeMnemonicIndicator(String string) {
		return LegacyActionTools.removeMnemonics(string);
	}
}
