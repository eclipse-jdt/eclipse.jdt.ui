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

import java.util.Arrays;
import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Helper class to provide String manipulation functions not available in standard JDK.
 */
public class Strings {
	
	private Strings(){}
	
	/**
	 * Indent char is a space char but not a line delimiters.
	 * <code>== Character.isWhitespace(ch) && ch != '\n' && ch != '\r'</code>
	 */
	public static boolean isIndentChar(char ch) {
		return Character.isWhitespace(ch) && !isLineDelimiterChar(ch);
	}
	
	/**
	 * tests if a char is lower case. Fix for 26529 
	 */
	public static boolean isLowerCase(char ch) {
		return Character.toLowerCase(ch) == ch;
	}	
	
	/**
	 * Line delimiter chars are  '\n' and '\r'.
	 */
	public static boolean isLineDelimiterChar(char ch) {
		return ch == '\n' || ch == '\r';
	}	

	public static String removeNewLine(String message) {
		StringBuffer result= new StringBuffer();
		int current= 0;
		int index= message.indexOf('\n', 0);
		while (index != -1) {
			result.append(message.substring(current, index));
			if (current < index && index != 0)
				result.append(' ');
			current= index + 1;
			index= message.indexOf('\n', current);
		}
		result.append(message.substring(current));
		return result.toString();
	}

	/**
	 * Converts the given string into an array of lines. The lines 
	 * don't contain any line delimiter characters.
	 *
	 * @return the string converted into an array of strings. Returns <code>
	 * 	null</code> if the input string can't be converted in an array of lines.
	 */
	public static String[] convertIntoLines(String input) {
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(input);
			int size= tracker.getNumberOfLines();
			String result[]= new String[size];
			for (int i= 0; i < size; i++) {
				IRegion region= tracker.getLineInformation(i);
				int offset= region.getOffset();
				result[i]= input.substring(offset, offset + region.getLength());
			}
			return result;
		} catch (BadLocationException e) {
			return null;
		}
	}

	/**
	 * Returns <code>true</code> if the given string only consists of
	 * white spaces according to Java. If the string is empty, <code>true
	 * </code> is returned.
	 * 
	 * @return <code>true</code> if the string only consists of white
	 * 	spaces; otherwise <code>false</code> is returned
	 * 
	 * @see java.lang.Character#isWhitespace(char)
	 */
	public static boolean containsOnlyWhitespaces(String s) {
		int size= s.length();
		for (int i= 0; i < size; i++) {
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		}
		return true;
	}
	
	/**
	 * Removes leading tabs and spaces from the given string. If the string
	 * doesn't contain any leading tabs or spaces then the string itself is 
	 * returned.
	 */
	public static String trimLeadingTabsAndSpaces(String line) {
		int size= line.length();
		int start= size;
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (!isIndentChar(c)) {
				start= i;
				break;
			}
		}
		if (start == 0)
			return line;
		else if (start == size)
			return ""; //$NON-NLS-1$
		else
			return line.substring(start);
	}
	
	public static String trimTrailingTabsAndSpaces(String line) {
		int size= line.length();
		int end= size;
		for (int i= size - 1; i >= 0; i--) {
			char c= line.charAt(i);
			if (isIndentChar(c)) {
				end= i;
			} else {
				break;
			}
		}
		if (end == size)
			return line;
		else if (end == 0)
			return ""; //$NON-NLS-1$
		else
			return line.substring(0, end);
	}
	
	/**
	 * Returns the indent of the given string.
	 * 
	 * @param line the text line
	 * @param tabWidth the width of the '\t' character.
	 * @deprecated use {@link #computeIndentUnits(String, int, int)} instead
	 */
	public static int computeIndent(String line, int tabWidth) {
		return computeIndentUnits(line, tabWidth, tabWidth);
	}
	
	/**
	 * Returns the indent of the given string in indentation units. Odd spaces
	 * are not counted.
	 * 
	 * @param line the text line
	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @since 3.1
	 */
	public static int computeIndentUnits(String line, IJavaProject project) {
		return computeIndentUnits(line, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project));
	}
	
	/**
	 * Returns the indent of the given string in indentation units. Odd spaces
	 * are not counted.
	 * 
	 * @param line the text line
	 * @param tabWidth the width of the '\t' character in space equivalents
	 * @param indentWidth the width of one indentation unit in space equivalents
	 * @since 3.1
	 */
	public static int computeIndentUnits(String line, int tabWidth, int indentWidth) {
		if (indentWidth == 0)
			return -1;
		int visualLength= measureIndentLength(line, tabWidth);
		return visualLength / indentWidth;
	}
	
	/**
	 * Computes the visual length of the indentation of a
	 * <code>CharSequence</code>, counting a tab character as the size until
	 * the next tab stop and every other whitespace character as one.
	 * 
	 * @param line the string to measure the indent of
	 * @param tabSize the visual size of a tab in space equivalents
	 * @return the visual length of the indentation of <code>line</code>
	 * @since 3.1
	 */
	public static int measureIndentLength(CharSequence line, int tabSize) {
		int length= 0;
		int max= line.length();
		for (int i= 0; i < max; i++) {
			char ch= line.charAt(i);
			if (ch == '\t') {
				int reminder= length % tabSize;
				length += tabSize - reminder;
			} else if (isIndentChar(ch)) {
				length++;
			} else {
				return length;
			}
		}
		return length;
	}

	/**
	 * Removes the given number of indents from the line. Asserts that the given line 
	 * has the requested number of indents. If <code>indentsToRemove <= 0</code>
	 * the line is returned.
	 * 
	 * @deprecated as of 3.1 use {@link #trimIndent(String, int, int, int)} instead
	 */
	public static String trimIndent(String line, int indentsToRemove, int tabWidth) {
		return trimIndent(line, indentsToRemove, tabWidth, tabWidth);
	}
	
	/**
	 * Removes the given number of indents from the line. Asserts that the given line 
	 * has the requested number of indents. If <code>indentsToRemove <= 0</code>
	 * the line is returned.
	 * 
	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @since 3.1
	 */
	public static String trimIndent(String line, int indentsToRemove, IJavaProject project) {
		return trimIndent(line, indentsToRemove, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project));
	}
	
	/**
	 * Removes the given number of indents from the line. Asserts that the given line 
	 * has the requested number of indents. If <code>indentsToRemove <= 0</code>
	 * the line is returned.
	 * 
	 * @since 3.1
	 */
	public static String trimIndent(String line, int indentsToRemove, int tabWidth, int indentWidth) {
		if (line == null || indentsToRemove <= 0)
			return line;

		final int spaceEquivalentsToRemove= indentsToRemove * indentWidth;
		
		int start= 0;
		int spaceEquivalents= 0;
		int size= line.length();
		String prefix= null;
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c == '\t') {
				int remainder= spaceEquivalents % tabWidth;
				spaceEquivalents += tabWidth - remainder;
			} else if (isIndentChar(c)) {
				spaceEquivalents++;
			} else {
				// Assert.isTrue(false, "Line does not have requested number of indents"); //$NON-NLS-1$
				start= i;
				break; 
			}
			if (spaceEquivalents == spaceEquivalentsToRemove) {
				start= i + 1;
				break;
			}
			if (spaceEquivalents > spaceEquivalentsToRemove) {
				// can happen if tabSize > indentSize, e.g tabsize==8, indent==4, indentsToRemove==1, line prefixed with one tab
				// this implements the third option
				start= i + 1; // remove the tab
				// and add the missing spaces
				char[] missing= new char[spaceEquivalents - spaceEquivalentsToRemove];
				Arrays.fill(missing, ' ');
				prefix= new String(missing);
				break;
			}
		}
		String trimmed;
		if (start == size)
			trimmed= ""; //$NON-NLS-1$
		else
			trimmed= line.substring(start);
		
		if (prefix == null)
			return trimmed;
		return prefix + trimmed;
	}

	/**
	 * Removes the common number of indents from all lines. If a line
	 * only consists out of white space it is ignored.
	 * @deprecated as of 3.1 use {@link #trimIndentation(String[], int, int)} instead
	 */
	public static void trimIndentation(String[] lines, int tabWidth) {
		trimIndentation(lines, tabWidth, tabWidth, true);
	}
	
	/**
	 * Removes the common number of indents from all lines. If a line
	 * only consists out of white space it is ignored.

	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @since 3.1
	 */
	public static void trimIndentation(String[] lines, IJavaProject project) {
		trimIndentation(lines, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project), true);
	}
	/**
	 * Removes the common number of indents from all lines. If a line
	 * only consists out of white space it is ignored.
	 * 
	 * @since 3.1
	 */
	public static void trimIndentation(String[] lines, int tabWidth, int indentWidth) {
		trimIndentation(lines, tabWidth, indentWidth, true);
	}
	
	/**
	 * Removes the common number of indents from all lines. If a line
	 * only consists out of white space it is ignored. If <code>
	 * considerFirstLine</code> is false the first line will be ignored.
	 * @deprecated as of 3.1 use {@link #trimIndentation(String[], int, int, boolean)} instead
	 */
	public static void trimIndentation(String[] lines, int tabWidth, boolean considerFirstLine) {
		trimIndentation(lines, tabWidth, tabWidth, considerFirstLine);
	}
	
	/**
	 * Removes the common number of indents from all lines. If a line
	 * only consists out of white space it is ignored. If <code>
	 * considerFirstLine</code> is false the first line will be ignored.
	 * 
	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @since 3.1
	 */
	public static void trimIndentation(String[] lines, IJavaProject project, boolean considerFirstLine) {
		trimIndentation(lines, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project), considerFirstLine);
	}
	
	/**
	 * Removes the common number of indents from all lines. If a line
	 * only consists out of white space it is ignored. If <code>
	 * considerFirstLine</code> is false the first line will be ignored.
	 * @since 3.1
	 */
	public static void trimIndentation(String[] lines, int tabWidth, int indentWidth, boolean considerFirstLine) {
		String[] toDo= new String[lines.length];
		// find indentation common to all lines
		int minIndent= Integer.MAX_VALUE; // very large
		for (int i= considerFirstLine ? 0 : 1; i < lines.length; i++) {
			String line= lines[i];
			if (containsOnlyWhitespaces(line))
				continue;
			toDo[i]= line;
			int indent= computeIndentUnits(line, tabWidth, indentWidth);
			if (indent < minIndent) {
				minIndent= indent;
			}
		}
		
		if (minIndent > 0) {
			// remove this indent from all lines
			for (int i= considerFirstLine ? 0 : 1; i < toDo.length; i++) {
				String s= toDo[i];
				if (s != null)
					lines[i]= trimIndent(s, minIndent, tabWidth, indentWidth);
				else {
					String line= lines[i];
					int indent= computeIndentUnits(line, tabWidth, indentWidth);
					if (indent > minIndent)
						lines[i]= trimIndent(line, minIndent, tabWidth, indentWidth);
					else
						lines[i]= trimLeadingTabsAndSpaces(line);
				}
			}
		}
	}
	
	/**
	 * Returns that part of the indentation of <code>line</code> that makes up
	 * a multiple of indentation units.
	 * 
	 * @param line the line to scan
	 * @param tabWidth the size of one tab in space equivalents
	 * @return the indent part of <code>line</code>, but no odd spaces
	 * @deprecated as of 3.1 use {@link #getIndentString(String, int, int)} instead
	 */
	public static String getIndentString(String line, int tabWidth) {
		return getIndentString(line, tabWidth, tabWidth);
	}
	
	/**
	 * Returns that part of the indentation of <code>line</code> that makes up
	 * a multiple of indentation units.
	 * 
	 * @param line the line to scan
	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @return the indent part of <code>line</code>, but no odd spaces
	 * @since 3.1
	 */
	public static String getIndentString(String line, IJavaProject project) {
		return getIndentString(line, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project));
	}
	
	/**
	 * Returns that part of the indentation of <code>line</code> that makes up
	 * a multiple of indentation units.
	 * 
	 * @param line the line to scan
	 * @param tabWidth the size of one tab in space equivalents
	 * @param indentWidth the size of the indent in space equivalents
	 * @return the indent part of <code>line</code>, but no odd spaces
	 * @since 3.1
	 */
	public static String getIndentString(String line, int tabWidth, int indentWidth) {
		int size= line.length();
		int end= 0;
		
		int spaceEquivs= 0;
		int characters= 0;
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c == '\t') {
				int remainder= spaceEquivs % tabWidth;
				spaceEquivs += tabWidth - remainder;
				characters++;
			} else if (isIndentChar(c)) {
				spaceEquivs++;
				characters++;
			} else {
				break;
			}
			if (spaceEquivs >= indentWidth) {
				end += characters;
				characters= 0;
				spaceEquivs= spaceEquivs % indentWidth;
			}
		}
		if (end == 0)
			return ""; //$NON-NLS-1$
		else if (end == size)
			return line;
		else
			return line.substring(0, end);
	}
	
	/**
	 * Returns the length of the string representing the number of 
	 * indents in the given string <code>line</code>. Returns 
	 * <code>-1<code> if the line isn't prefixed with an indent of
	 * the given number of indents. 
	 * @deprecated as of 3.1 use {@link #computeIndentLength(String, int, int, int)} instead
	 */
	public static int computeIndentLength(String line, int numberOfIndents, int tabWidth) {
		return computeIndentLength(line, numberOfIndents, tabWidth, tabWidth);
	}
	
	/**
	 * Returns the length of the string representing the number of 
	 * indents in the given string <code>line</code>. Returns 
	 * <code>-1<code> if the line isn't prefixed with an indent of
	 * the given number of indents.
	 * 
	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @since 3.1
	 */
	public static int computeIndentLength(String line, int numberOfIndents, IJavaProject project) {
		return computeIndentLength(line, numberOfIndents, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project));
	}
	
	/**
	 * Returns the length of the string representing the number of 
	 * indents in the given string <code>line</code>. Returns 
	 * <code>-1<code> if the line isn't prefixed with an indent of
	 * the given number of indents.
	 * @since 3.1
	 */
	public static int computeIndentLength(String line, int numberOfIndents, int tabWidth, int indentWidth) {
		Assert.isTrue(numberOfIndents >= 0);
		Assert.isTrue(tabWidth >= 0);
		Assert.isTrue(indentWidth >= 0);
		
		int spaceEquivalents= numberOfIndents * indentWidth;
		
		int size= line.length();
		int result= -1;
		int blanks= 0;
		for (int i= 0; i < size && blanks < spaceEquivalents; i++) {
			char c= line.charAt(i);
			if (c == '\t') {
				int remainder= blanks % tabWidth;
				blanks += tabWidth - remainder;
			} else if (isIndentChar(c)) {
				blanks++;
			} else {
				break;
			}
			result= i;
		}
		if (blanks < spaceEquivalents)
			return -1;
		return result + 1;
	}
	
	public static String[] removeTrailingEmptyLines(String[] sourceLines) {
		int lastNonEmpty= findLastNonEmptyLineIndex(sourceLines);
		String[] result= new String[lastNonEmpty + 1];
		for (int i= 0; i < result.length; i++) {
			result[i]= sourceLines[i];
		}
		return result;
	}

	private static int findLastNonEmptyLineIndex(String[] sourceLines) {
		for (int i= sourceLines.length - 1; i >= 0; i--) {
			if (! sourceLines[i].trim().equals(""))//$NON-NLS-1$
				return i;
		}
		return -1;
	}
	
	/**
	 * Change the indent of, possible muti-line, code range. The current indent is removed, a new indent added.
	 * The first line of the code will not be changed. (It is considered to have no indent as it might start in
	 * the middle of a line)
	 * @deprecated use the version specifying the indent width instead
	 */
	public static String changeIndent(String code, int codeIndentLevel, int tabWidth, String newIndent, String lineDelim) {
		return changeIndent(code, codeIndentLevel, tabWidth, tabWidth, newIndent, lineDelim);
	}
	
	/**
	 * Change the indent of, possible muti-line, code range. The current indent is removed, a new indent added.
	 * The first line of the code will not be changed. (It is considered to have no indent as it might start in
	 * the middle of a line)
	 * 
	 * @param project the java project from which to get the formatter
	 *        preferences, or <code>null</code> for global preferences
	 * @since 3.1
	 */
	public static String changeIndent(String code, int codeIndentLevel, IJavaProject project, String newIndent, String lineDelim) {
		return changeIndent(code, codeIndentLevel, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project), newIndent, lineDelim);
	}
	
	/**
	 * Change the indent of, possible muti-line, code range. The current indent is removed, a new indent added.
	 * The first line of the code will not be changed. (It is considered to have no indent as it might start in
	 * the middle of a line)
	 * @since 3.1
	 */
	public static String changeIndent(String code, int codeIndentLevel, int tabWidth, int indentWidth, String newIndent, String lineDelim) {
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(code);
			int nLines= tracker.getNumberOfLines();
			if (nLines == 1) {
				return code;
			}
			
			StringBuffer buf= new StringBuffer();
			
			for (int i= 0; i < nLines; i++) {
				IRegion region= tracker.getLineInformation(i);
				int start= region.getOffset();
				int end= start + region.getLength();
				String line= code.substring(start, end);
				
				if (i == 0) {  // no indent for first line (contained in the formatted string)
					buf.append(line);
				} else { // no new line after last line
					buf.append(lineDelim);
					buf.append(newIndent); 
					buf.append(trimIndent(line, codeIndentLevel, tabWidth, indentWidth));
				}
			}
			return buf.toString();
		} catch (BadLocationException e) {
			// can not happen
			return code;
		}
	}
	
	/**
	 * @deprecated use {@link #trimIndentation(String, int, int, boolean)} instead
	 */
	public static String trimIndentation(String source, int tabWidth, boolean considerFirstLine) {
		return trimIndentation(source, tabWidth, tabWidth, considerFirstLine);
	}
	
	public static String trimIndentation(String source, IJavaProject project, boolean considerFirstLine) {
		return trimIndentation(source, CodeFormatterUtil.getTabWidth(project), CodeFormatterUtil.getIndentWidth(project), considerFirstLine);
	}
	
	public static String trimIndentation(String source, int tabWidth, int indentWidth, boolean considerFirstLine) {
		try {
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(source);
			int size= tracker.getNumberOfLines();
			if (size == 1)
				return source;
			String lines[]= new String[size];
			for (int i= 0; i < size; i++) {
				IRegion region= tracker.getLineInformation(i);
				int offset= region.getOffset();
				lines[i]= source.substring(offset, offset + region.getLength());
			}
			Strings.trimIndentation(lines, tabWidth, indentWidth, considerFirstLine);
			StringBuffer result= new StringBuffer();
			int last= size - 1;
			for (int i= 0; i < size; i++) {
				result.append(lines[i]);
				if (i < last)
					result.append(tracker.getLineDelimiter(i));
			}
			return result.toString();
		} catch (BadLocationException e) {
			Assert.isTrue(false,"Can not happend"); //$NON-NLS-1$
			return null;
		}
	}
		
	
	/**
	 * Concatenate the given strings into one strings using the passed line delimiter as a
	 * delimiter. No delimiter is added to the last line.
	 */
	public static String concatenate(String[] lines, String delimiter) {
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < lines.length; i++) {
			if (i > 0)
				buffer.append(delimiter);
			buffer.append(lines[i]);
		}
		return buffer.toString();
	}
	
	public static boolean equals(String s, char[] c) {
		if (s.length() != c.length)
			return false;

		for (int i = c.length; --i >= 0;)
			if (s.charAt(i) != c[i])
				return false;
		return true;
	}
	
	public static String[] splitByToken(String fullString, String splitToken) {
		StringTokenizer tokenizer= new StringTokenizer(fullString, splitToken);
		String[] tokens= new String[tokenizer.countTokens()];
		for (int i= 0; tokenizer.hasMoreTokens(); i++) {
			tokens[i]= tokenizer.nextToken();
		}
		return tokens;
	}
}

