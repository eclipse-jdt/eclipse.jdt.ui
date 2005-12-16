package p;

public class A {

	void foo() {
		
	}

	/**
	 * @deprecated Use {@link #bar()} instead
	 */
	void foo() {
		bar();
	}
}
