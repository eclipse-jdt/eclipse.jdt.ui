package junit.runner;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import java.util.*;

/**
 * A custom quick sort with support to customize the swap behaviour. NOTICE: We
 * can't use the the sorting support from the JDK 1.2 collection classes because
 * of the JDK 1.1.7 compatibility.
 */
public class Sorter {
	public interface Swapper {
		void swap(Vector<?> values, int left, int right);
	}

	public static void sortStrings(Vector<?> values, int left, int right, Swapper swapper) {
		int oleft = left;
		int oright = right;
		String mid = (String) values.elementAt((left + right) / 2);
		do {
			while (((String) (values.elementAt(left))).compareTo(mid) < 0)
				left++;
			while (mid.compareTo((String) (values.elementAt(right))) < 0)
				right--;
			if (left <= right) {
				swapper.swap(values, left, right);
				left++;
				right--;
			}
		} while (left <= right);

		if (oleft < right)
			sortStrings(values, oleft, right, swapper);
		if (left < oright)
			sortStrings(values, left, oright, swapper);
	}
}
