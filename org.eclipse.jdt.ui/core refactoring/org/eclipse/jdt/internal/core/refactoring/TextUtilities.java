/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.jdt.core.IBuffer;

public class TextUtilities {

	/**
	 * Returns the length of the string desribed by <code>start</code> and 
	 * <code>end</code>.
	 *
	 * @param start the start position. The position is inclusive
	 * @param end the end position. The position is inclusive
	 * @return the length of the string desribed by <code>start</code> and
	 *  <code>end</code>
	 */
	public static int getLength(int start, int end) {
		return end - start + 1;
	}

	/**
	 * Returns the indent of the given line.
	 * @param line the text line
	 * @param tabWidth the width of the '\t' character.
	 */
	public static int getIndent(String line, int tabWidth) {
		int result= 0;
		int blanks= 0;
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			switch (c) {
				case '\t':
					result++;
					blanks= 0;
					break;
				case	' ':
					blanks++;
					if (blanks == tabWidth) {
						result++;
						blanks= 0;
					}
					break;
				default:
					return result;
			}
		}
		return result;
	}
	
	/**
	 * Removes the given number of idents from the line and returns a new
	 * copy of the line. Asserts that the given line has the requested
	 * number of indents.
	 */
	public static String removeIndent(int numberOfIndents, String line, int tabWidth) {
		if (numberOfIndents <= 0)
			return new String(line);
			
		int start= 0;
		int indents= 0;
		int blanks= 0;
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			switch (c) {
				case '\t':
					indents++;
					blanks= 0;
					break;
				case ' ':
					blanks++;
					if (blanks == tabWidth) {
						indents++;
						blanks= 0;
					}
					break;
				default:
					Assert.isTrue(false, "Line doesn't have requested number of indents");
			}
			if (indents == numberOfIndents) {
				start= i + 1;
				break;
			}	
		}
		if (start == size)
			return "";
		else
			return line.substring(start);
	}
	
	/**
	 * Removes any leading indents from the given string.
	 */
	public static String removeLeadingIndents(String line, int tabWidth) {
		int indents= getIndent(line, tabWidth);
		return removeIndent(indents, line, tabWidth);
	}
	 
	/**
	 * Creates a string that consists of the given number of tab characters.
	 */
	public static String createIndentString(int indent) {
		StringBuffer result= new StringBuffer();
		for (int i= 0; i < indent; i++) {
			result.append('\t');
		}
		return result.toString();
	} 
	
	/**
	 * Removes any leading white spaces from the given string.
	 * The method returns a new string.
	 */
	public static String removeLeadingWhiteSpaces(String line) {
		int size= line.length();
		int start= 0;
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c != '\t' && c != ' ') {
				start= i;
				break;
			}
		}
		return line.substring(start);
	}
	
	/**
	 * Returns <code>true</code> if the given string consists only of
	 * white spaces (e.g. space and '\t'). If the string is empty,
	 * <code>true</code> is returned.
	 */
	public static boolean containsOnlyWhiteSpaces(String line) {
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c != '\t' && c != ' ')
				return false;
		}
		return true;
	}
	
	/**
	 * Tests if the given array of lines ends with a semicolon in a Java language sense.
	 * This means that text like "; \/\* comment \*\/" ends with a semicolon.
	 */
	public static boolean endsWithSemicolon(String[] lines) {
		boolean inComment= false;
		boolean result= false;
		nextLine: for (int z= 0; z < lines.length; z++) {
			String line= lines[z];
			int length= line.length();
			for (int i= 0; i < length; i++) {
				char c= line.charAt(i);
				switch (c) {
					case ';':
						if (!inComment)
							result= true;
						break;
					case '/':
						if (i + 1 < length) {
							char nextChar= line.charAt(i + 1);
							if (nextChar == '*') {
								inComment= true;
								i++;
							}
							else if (nextChar == '/')
								continue nextLine;
						}	
						break;
					case '*':
						if (i + 1 < length && line.charAt(i + 1) == '/') {
							inComment= false;
							i++;
						}
						break;
					case ' ':
					case '\t':
						break;
					default:
						if (!inComment)
							result= false;
				}
			}
		}
		return result;
	}	
}