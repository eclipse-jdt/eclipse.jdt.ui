/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

/**
 * Helper class to provide String manipulation functions not available in standard JDK.
 */
public class Strings {

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

