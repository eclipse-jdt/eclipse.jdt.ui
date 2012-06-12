package p;
class B {
	/** Comment */
	private final A a;

	/**
	 * @param a
	 */
	B(A a) {
		this.a= a;
	}

	public void execute() {
		B b = p.B.this;
		synchronized (this.a) {
			System.err.println();
		}
	}
}