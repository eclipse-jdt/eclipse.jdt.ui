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

class A {
	public void m(Object o, int i, String... names) {
		for (String name : names) {
			System.out.println(name);
		}
	}
}

class B extends A {
	public void m(Object o, int i, String[] names) {
		for (String name : names) {
			System.out.println(name);
		}
	}
}

class Client {
	{
		new A().m(new Object(), 0);
		new A().m(new Object(), 2, "X", "Y", "Z");
		new B().m(new Object(), 1, new String[] {"X"});
	}
}