package p;
class Inner {
	private A a;

	/**
	 * @param a
	 */
	Inner(A a) {
		this.a= a;
		// TODO Auto-generated constructor stub
	}

	public void doit() {
		this.a.foo(this.a.bar());
	}
}