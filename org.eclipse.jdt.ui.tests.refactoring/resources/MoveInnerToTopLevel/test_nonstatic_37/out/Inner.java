package p;
class Inner {
	/** Comment */
	private A a;

	/**
	 * @param a
	 */
	Inner(A a) {
		this.a= a;
	}

	public void doit() {
		this.a.foo(this.a.bar());
	}
}