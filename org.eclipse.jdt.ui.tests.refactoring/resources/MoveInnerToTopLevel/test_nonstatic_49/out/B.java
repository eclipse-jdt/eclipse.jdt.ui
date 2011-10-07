package p;
class B<E> {
	/** Comment */
	private A<E> a;

	/**
	 * @param a
	 */
	B(A<E> a) {
		this.a= a;
	}

	void foo() {
		System.out.println(this.a.i);
	}
}