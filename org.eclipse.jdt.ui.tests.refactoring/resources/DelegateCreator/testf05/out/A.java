package p;

public class A {

	static String foo;
	/**
	 * @deprecated Use {@link B#foo} instead
	 */
	static String foo = B.foo;
}
