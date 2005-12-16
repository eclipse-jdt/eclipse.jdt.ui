package p;

public class A {

	static String foo;
	/**
	 * @deprecated Use {@link #foo} instead
	 */
	static String foo = foo;
}
