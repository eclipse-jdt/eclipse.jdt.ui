/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

import java.math.BigDecimal;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

class A {
	private SortedMap fillSortedMap(Vector values, boolean byValue) {
		TreeMap map= new TreeMap();
		for (int i= 0; i < values.size(); i += 2) {
			if (byValue) {
				map.put(
					values.get(i + 1),
					new Integer(((BigDecimal) values.get(i)).intValue()));
			} else {
				map.put(
					new Integer(((BigDecimal) values.get(i)).intValue()),
					values.get(i + 1));
			}
		}
		return map;
	}
}