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
	void foo() {
		List<Integer> l= new ArrayList<Integer>();
		l.add(new Integer(1));
		Number n= l.get(0);
		Object o1= l.get(0);
		Object o2= l.get(0);
		
		List<Number> l2= new ArrayList<Number>();
		l2.add(n);
		Integer i= (Integer) l2.get(0);
	}
}