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
	public void m(int i, String[] names) {
	}
}

class B extends A {
	public void m(int i, String... names) {
	}	
}

class Client {
	void test(int i, String... args) {
		new A().m(1, new String[] {"X"});
		new B().m(0);
		new B().m(2, "X", "Y", "Z");
	}
}
