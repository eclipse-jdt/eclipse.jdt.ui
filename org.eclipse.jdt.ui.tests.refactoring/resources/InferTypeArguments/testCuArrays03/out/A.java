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

import java.util.ArrayList;
import java.util.List;

class A {
	void addAll(String[] ss) {
		List<String> l= A.asList(ss);
	}
	
	public static <T> List<T> asList(T[] a) {
		ArrayList<T> res= new ArrayList<T>();
		for (int i= 0; i < a.length; i++) {
			res.add(a[i]);
		}
		return res;
	}
}