package p;
class Inner{
	private final A a;

	/**
	 * @param a
	 */
	Inner(A a) {
		this.a= a;
		// TODO Auto-generated constructor stub
	}

	void f(){
		this.a.m();
	}
}