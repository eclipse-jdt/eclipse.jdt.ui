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
	int i;
	void m(int i) {this.i = i;}
}
class Super extends A {
	void m(int i) { super.m(1); }
}
class Recursive extends A {
	void m(int i) { if (true) m(i); }
}
class ThisRecursive extends A {
	void m(int i) { this.m(i); }
}
class AlmostRecursive extends A {
	void m(int i) { new A().m(i); }
}
class RecursiveOrNot extends A {
	void m(int i) { new RecursiveOrNot().m(i); }
}
class NonRecursive extends A {
	void m(int i) { int k= i; }
}
class Calling extends A {
	void bar() { m(17); }
}
class Calling2 {
	void bar() { new A().m(17); }
}
class Calling3 {
	void bar() { new Recursive().m(17); }
}
