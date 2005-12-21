package p;

public class A {
	
	/**
	 * @deprecated Use {@link B#foo()} instead
	 */
	private static void foo() {
		B.foo();
	}

}
