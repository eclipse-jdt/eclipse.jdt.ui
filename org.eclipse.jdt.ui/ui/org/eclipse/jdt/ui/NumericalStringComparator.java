/*******************************************************************************
* Copyright (c) 2015, 2025 Katrina Hill and others.
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
*     Katrina Hill
*******************************************************************************/

package org.eclipse.jdt.ui;

/** Sorts alphabetically and then by numerical portion of strings having equivalent suffixes after the first '.'
 * If suffixes are not equal, falls back to default alphabetical sort
 * If prefixes excluding numerical portions are not equal, falls back to alphabetical sort
 * If either doesn't contain a numeric portion, it goes before the other
 * With all above false, parses numerical portion and sorts by that
 *
 * Example (unsorted):					Test1, Test20, Test3, Test, Test2
 * Default alphabetical sort: 			Test, Test1, Test2, Test20, Test3
 * Numerical Sort: 						Test, Test1, Test2, Test3, Test20
 *
 * @since 3.36
 */

public class NumericalStringComparator {

	public int compare(String s1, String s2) {
		int s1CompLength = s1.length();
		int s2CompLength = s2.length();
		//chop off file extensions to restrict comparison to filenames
		if(s1.contains(".")) { //$NON-NLS-1$
			s1CompLength = s1.indexOf('.');
		}
		if(s2.contains(".")) { //$NON-NLS-1$
			s2CompLength = s2.indexOf('.');
		}

		//sort numerically only if portion after first '.' is equal
		String s1Post = s1.substring(s1CompLength);
		String s2Post = s2.substring(s2CompLength);

		//only perform numerical sort if suffixes are equal, otherwise default to alphabetical
		if(!s1Post.equals(s2Post))
		{
			return s1.compareTo(s2);
		}

		//isolate numerical portions
		String s1Prefix = s1.substring(0, s1CompLength);
		String s2Prefix = s2.substring(0, s2CompLength);

		String ints = "0123456789"; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		int index = s1CompLength - 1;
		while(ints.contains(String.valueOf(s1.charAt(index))))
		{
			sb.append(s1.charAt(index));
			index--;
		}
		String s1NumStr = sb.reverse().toString();
		sb.setLength(0);
		index = s2CompLength - 1;
		while(ints.contains(String.valueOf(s2.charAt(index))))
		{
			sb.append(s2.charAt(index));
			index--;
		}
		String s2NumStr = sb.reverse().toString();

		s1Prefix = s1Prefix.substring(0, s1Prefix.length() - s1NumStr.length());
		s2Prefix = s2Prefix.substring(0, s2Prefix.length() - s2NumStr.length());

		//if non-numeric portion is sortable, return comparison now
		if(s1Prefix.compareTo(s2Prefix) != 0)
			return s1Prefix.compareTo(s2Prefix);

		//if non-numeric portion is equal, and suffix after first '.' is equal, compare numerical portions
		//if either is empty, return that as first item
		if(s1NumStr.length() == 0)
			return -1;

		if(s2NumStr.length() == 0)
			return 1;

		int s1Int = Integer.parseInt(s1NumStr);
		int s2Int = Integer.parseInt(s2NumStr);

		if(s1Int > s2Int)
			return 1;
		else if(s2Int > s1Int)
			return -1;
		else
			return 0;
	}
}
