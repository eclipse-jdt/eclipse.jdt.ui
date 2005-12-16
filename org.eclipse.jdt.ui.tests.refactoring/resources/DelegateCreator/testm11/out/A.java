package p;

public class A {

	A() { }

	/**
	 * @deprecated Use {@link #A()} instead
	 */
	A() {
		this();
	}
}
