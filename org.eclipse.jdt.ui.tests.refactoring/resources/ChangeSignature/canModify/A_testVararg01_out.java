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
	public void m(int i, String... strings) {
		for (String name : strings) {
			System.out.println(name);
		}
	}
}

class B extends A {
	public void m(int i, String[] strings) {
		for (String name : strings) {
			System.out.println(name);
		}
	}
}

class C extends B {
	public void m(int i, String... strings) {
		System.out.println(strings[i]);
		strings= new String[0];
	}
}

class Client {
	{
		new A().m(0);
		new B().m(1, new String[] {"X"});
		new C().m(2, new String[] {"X", "Y"});
		new C().m(2, "X", "Y", "Z");
	}
}