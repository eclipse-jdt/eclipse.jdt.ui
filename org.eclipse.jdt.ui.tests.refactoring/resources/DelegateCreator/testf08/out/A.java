package p;

import e.E;

public class A {

	static String foo;
	/**
	 * @deprecated Use {@link E#foo} instead
	 */
	static String foo = E.foo;
}
