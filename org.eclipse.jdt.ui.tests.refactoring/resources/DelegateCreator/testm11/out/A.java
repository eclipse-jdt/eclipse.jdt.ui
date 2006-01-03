package p;

public class A {

	/**
	 * @deprecated Use {@link #A()} instead
	 */
	A() {
		this();
	}

	A() { }
}
