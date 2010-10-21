/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/**
 * Helper class to convert between Java chars and the escaped form that must be used in .properties
 * files.
 * 
 * @since 3.7
 */
public class PropertiesFileEscapes {

	private static final char[] HEX_DIGITS= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private static char toHex(int halfByte) {
		return HEX_DIGITS[(halfByte & 0xF)];
	}

	/**
	 * Returns the decimal value of the Hex digit, or -1 if the digit is not a valid Hex digit.
	 * 
	 * @param digit the Hex digit
	 * @return the decimal value of digit, or -1 if digit is not a valid Hex digit.
	 */
	private static int getHexDigitValue(char digit) {
		switch (digit) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return digit - '0';
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
				return 10 + digit - 'a';
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
				return 10 + digit - 'A';
			default:
				return -1;
		}
	}

	/**
	 * Convert a Java char to the escaped form that must be used in .properties files.
	 * 
	 * @param c the Java char
	 * @return escaped string
	 */
	public static String escape(char c) {
		switch (c) {
			case '\b':
				return "\\b";//$NON-NLS-1$
			case '\t':
				return "\\t";//$NON-NLS-1$
			case '\n':
				return "\\n";//$NON-NLS-1$
			case '\f':
				return "\\f";//$NON-NLS-1$
			case '\r':
				return "\\r";//$NON-NLS-1$
			case '\\':
				return "\\\\";//$NON-NLS-1$

			default:
				if (((c < 0x0020) || (c > 0x007e && c <= 0x00a0) || (c > 0x00ff))) {
					//NBSP (0x00a0) is escaped to differentiate from normal space character
					return new StringBuffer()
							.append('\\')
							.append('u')
							.append(toHex((c >> 12) & 0xF))
							.append(toHex((c >> 8) & 0xF))
							.append(toHex((c >> 4) & 0xF))
							.append(toHex(c & 0xF)).toString();

				} else
					return String.valueOf(c);
		}
	}

	/**
	 * Convert an escaped string to a string composed of Java characters.
	 * 
	 * @param s the escaped string
	 * @return string composed of Java characters
	 * @throws CoreException if the escaped string has a malformed \\uxxx sequence
	 */
	public static String unescape(String s) throws CoreException {
		boolean isValidEscapedString= true;
		if (s == null)
			return null;

		char aChar;
		int len= s.length();
		StringBuffer outBuffer= new StringBuffer(len);

		for (int x= 0; x < len;) {
			aChar= s.charAt(x++);
			if (aChar == '\\') {
				if (x > len - 1) {
					return outBuffer.toString(); // silently ignore the \
				}
				aChar= s.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value= 0;
					if (x > len - 4) {
						String exceptionMessage= Messages.format(PropertiesFileEditorMessages.PropertiesFileHover_MalformedEncoding, outBuffer.toString() + s.substring(x - 2));
						throw new CoreException(new StatusInfo(IStatus.WARNING, exceptionMessage));
					}
					StringBuffer buf= new StringBuffer("\\u"); //$NON-NLS-1$
					int digit= 0;
					for (int i= 0; i < 4; i++) {
						aChar= s.charAt(x++);
						digit= getHexDigitValue(aChar);
						if (digit == -1) {
							isValidEscapedString= false;
							x--;
							break;
						}
						value= (value << 4) + digit;
						buf.append(aChar);
					}
					outBuffer.append(digit == -1 ? buf.toString() : String.valueOf((char)value));
				} else if (aChar == 'b') {
					outBuffer.append('\b');
				} else if (aChar == 't') {
					outBuffer.append('\t');
				} else if (aChar == 'n') {
					outBuffer.append('\n');
				} else if (aChar == 'f') {
					outBuffer.append('\f');
				} else if (aChar == 'r') {
					outBuffer.append('\r');
				} else {
					outBuffer.append(aChar); // silently ignore the \
				}
			} else
				outBuffer.append(aChar);
		}
		if (isValidEscapedString) {
			return outBuffer.toString();
		}
		else {
			String exceptionMessage= Messages.format(PropertiesFileEditorMessages.PropertiesFileHover_MalformedEncoding, outBuffer.toString());
			throw new CoreException(new StatusInfo(IStatus.WARNING, exceptionMessage));
		}
	}
}
