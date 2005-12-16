package p;

public class A {

	@Anno1
	@Anno2
	static String foo;
	/**
	 * @deprecated Use {@link #foo} instead
	 */
	@Anno1
	@Anno2
	static String foo = foo;
}
