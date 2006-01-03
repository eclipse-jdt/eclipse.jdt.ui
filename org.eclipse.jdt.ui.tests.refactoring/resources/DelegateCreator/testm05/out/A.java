package p;

public class A {

	/**
	 * @deprecated Use {@link B#foo()} instead
	 */
	static void foo() {
		B.foo();
	}

	static void foo() {
		
	}
}
