package p;

import e.E;

public class A {

	/**
	 * @deprecated Use {@link E#foo()} instead
	 */
	static void foo() {
		E.foo();
	}

	static void foo() { }
}
