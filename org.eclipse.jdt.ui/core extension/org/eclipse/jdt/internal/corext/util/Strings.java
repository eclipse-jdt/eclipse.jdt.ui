/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

/**
 * Helper class to provide String manipulation functions not available in standard JDK.
 */
public class Strings {

	/**
	 * Returns <code>true</code> if the given two strings are equal.
	 * 
	 * @return <code>true</code> if the given two strings are equal; <code>
	 * 	false</code> otherwise
	 */
	public static boolean equals(String s1, char[] s2) {
		if (s1.length() != s2.length)
			return false;
		for (int i= 0; i < s2.length; i++) {
			if (s1.charAt(i) != s2[i])
				return false;
		}
		return true;
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
	
}

