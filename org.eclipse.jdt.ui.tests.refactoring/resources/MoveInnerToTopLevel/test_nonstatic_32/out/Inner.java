package p;
class Inner{
	/** Comment */
	private final A a;

	/**
	 * @param a
	 */
	Inner(A a) {
		this.a= a;
	}

	void f(){
		this.a.m= 1;
	}
}