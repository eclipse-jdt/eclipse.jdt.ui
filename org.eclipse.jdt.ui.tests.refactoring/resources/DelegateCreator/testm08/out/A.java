package p;

import e.E;

public class A {

	static void foo() { }

	/**
	 * @deprecated Use {@link E#foo()} instead
	 */
	static void foo() {
		E.foo();
	}
}
