package p;


public class A {
	
	/**
	 * @deprecated Use {@link B#foo()} instead
	 */
	public static void foo() {
		B.foo();
	}

}
