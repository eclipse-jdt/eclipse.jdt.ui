/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/


package org.eclipse.jdt.internal.ui.text.correction;

public class NameMatcher {
	
	/**
	 * Returns a similarity value of the two names.
	 * The range of is from 0 to 256. no similarity is negative
	 */
	public static boolean isSimilarName(String name1, String name2) {
		return getSimilarity(name1, name2) >= 0;
	}	
		
	/**
	 * Returns a similarity value of the two names.
	 * The range of is from 0 to 256. no similarity is negative
	 */
	public static int getSimilarity(String name1, String name2) {	
		if (name1.length() > name2.length()) {
			String tmp= name1;
			name1= name2;
			name2= tmp;
		}
		int name1len= name1.length();
		int name2len= name2.length();
		
		int nMatched= 0;
		
		int i= 0;
		while (i < name1len && isSimilarChar(name1.charAt(i), name2.charAt(i))) {
			i++;
			nMatched++;
		}
		
		int k= name1len;
		int diff= name2len - name1len;
		while (k > i && isSimilarChar(name1.charAt(k - 1), name2.charAt(k + diff - 1))) {
			k--;
			nMatched++;
		}
		
		if (nMatched == name2len) {
			return 200;
		}
		
		if (name2len - nMatched > nMatched) {
			return -1;
		}
		
		int tolerance= name2len / 4 + 1;
		return (tolerance - (k - i)) * 256 / tolerance;
	}
	
	private static boolean isSimilarChar(char ch1, char ch2) {
		return Character.toLowerCase(ch1) == Character.toLowerCase(ch2);
	}
	
	private static void test(String name1, String name2) {
		int sim= getSimilarity(name1, name2);
		System.out.println(name1 + " - " + name2 + " : " + sim); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static void main(String[] arguments) {
		test("runner", "gunner"); //$NON-NLS-1$ //$NON-NLS-2$
		test("rundner", "gunner"); //$NON-NLS-1$ //$NON-NLS-2$
		test("rundner", "rund"); //$NON-NLS-1$ //$NON-NLS-2$
		test("test", "rund"); //$NON-NLS-1$ //$NON-NLS-2$
	}


}
