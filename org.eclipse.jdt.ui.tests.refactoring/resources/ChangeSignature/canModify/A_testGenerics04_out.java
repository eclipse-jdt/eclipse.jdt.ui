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

import java.util.List;

class A<E> {
	public <T extends Number> void m(A<String> as, List<Integer> li) {}
}

class Sub<E> extends A<E> {
	public <T> void m(A<String> as, List<Integer> li) {}
	
	void test() {
		A<String> as= new A<String>();
		as.m(as, null);
		new Sub<Double>().m(as, null);
	}
}