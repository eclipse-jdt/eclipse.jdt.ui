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

class A<E> {
	public <T extends Number> void m(List<Integer> li, A<String> as) {}
}

class Sub<E> extends A<E> {
	public <T> void m(List<Integer> li, A<String> as) {}
	
	void test() {
		A<String> as= new A<String>();
		as.m(new ArrayList<Integer>(1), as);
		new Sub<Double>().m(new ArrayList<Integer>(2), as);
	}
}