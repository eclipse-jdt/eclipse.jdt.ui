package p;

public class A {

	/**
	 * @deprecated Use {@link B#bar()} instead
	 */
	static void foo() {
		B.bar();
	}

	static void foo() {
		
	}
}
