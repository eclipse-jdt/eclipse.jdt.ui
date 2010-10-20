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

/**
 * Helper class to perform Native to ASCII conversion and vice versa.
 * 
 * @since 3.7
 */
public class NativeToAscii {

	private static final char[] HEX_DIGITS= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private static char toHex(int halfByte) {
		return HEX_DIGITS[(halfByte & 0xF)];
	}

	public static String getEscapedAsciiString(char c) {
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

	public static String getNativeString(String s) {
		if (s == null)
			return null;

		char aChar;
		int len= s.length();
		StringBuffer outBuffer= new StringBuffer(len);

		for (int x= 0; x < len;) {
			aChar= s.charAt(x++);
			if (aChar == '\\') {
				if (x > len - 1) {
					return outBuffer.toString();
				}
				aChar= s.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value= 0;
					if (x > len - 4) {
						return outBuffer.toString() + s.substring(x - 2);
					}
					StringBuffer buf= new StringBuffer("\\u"); //$NON-NLS-1$
					boolean invalid= false;
					for (int i= 0; i < 4; i++) {
						aChar= s.charAt(x++);
						buf.append(aChar);
						switch (aChar) {
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
								value= (value << 4) + aChar - '0';
								break;
							case 'a':
							case 'b':
							case 'c':
							case 'd':
							case 'e':
							case 'f':
								value= (value << 4) + 10 + aChar - 'a';
								break;
							case 'A':
							case 'B':
							case 'C':
							case 'D':
							case 'E':
							case 'F':
								value= (value << 4) + 10 + aChar - 'A';
								break;
							default:
								invalid= true;
						}
					}
					outBuffer.append(invalid ? buf.toString() : String.valueOf((char)value));
				} else if (aChar == 't') {
					outBuffer.append('\t');
				} else if (aChar == 'r') {
					outBuffer.append('\r');
				} else if (aChar == 'n') {
					outBuffer.append('\n');
				} else if (aChar == 'f') {
					outBuffer.append('\f');
				} else {
					outBuffer.append(aChar);
				}
			} else
				outBuffer.append(aChar);
		}
		return outBuffer.toString();
	}

	public static boolean isValidNativeString(String s) {
		if (s.indexOf("\\u") != -1) //$NON-NLS-1$
			return false;
		return true;

	}
}
