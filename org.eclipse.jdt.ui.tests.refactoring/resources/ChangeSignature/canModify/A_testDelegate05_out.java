package p;

class A {
	/**
	 * @deprecated Use {@link #renamed()} instead
	 */
	void m() {
		renamed();
	}
	void renamed() {}
	void ref() {
		renamed();
	}
}
