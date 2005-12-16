package p;

public class A {

	static void foo() {
		
	}

	/**
	 * @deprecated Use {@link B#foo()} instead
	 */
	static void foo() {
		B.foo();
	}
}
