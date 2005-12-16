package p;

public class A {

	void foo() {
		
	}

	/**
	 * @deprecated Use {@link #foo()} instead
	 */
	void foo() {
		foo();
	}
}
