package p;

public class A {
	void k() { }

	/**
	 * @deprecated Use {@link #k()} instead
	 */
	void m() {
		k();
	}
}
