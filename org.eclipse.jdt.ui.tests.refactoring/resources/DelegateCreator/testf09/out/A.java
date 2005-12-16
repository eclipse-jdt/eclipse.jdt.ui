package p;

public class A {

	static String foo = "Tell me something";
	/**
	 * @deprecated Use {@link #foo} instead
	 */
	static String foo = foo;
}
