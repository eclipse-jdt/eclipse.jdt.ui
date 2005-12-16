package p;

public class A {

	static String foo;
	/**
	 * @deprecated Use {@link B#bar} instead
	 */
	static String foo = B.bar;
}
