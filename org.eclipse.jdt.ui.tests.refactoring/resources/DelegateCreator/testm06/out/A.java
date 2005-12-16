package p;

public class A {

	static void foo() {
		
	}

	/**
	 * @deprecated Use {@link B#bar()} instead
	 */
	static void foo() {
		B.bar();
	}
}
