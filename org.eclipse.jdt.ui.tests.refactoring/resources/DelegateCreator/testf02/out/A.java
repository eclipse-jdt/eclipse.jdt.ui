package p;

public class A {

	/**
	 * This is the field "foo".
	 */
	static String foo;
	/**
	 * This is the field "foo".
	 * @deprecated Use {@link #foo} instead
	 */
	static String foo = foo;
}
