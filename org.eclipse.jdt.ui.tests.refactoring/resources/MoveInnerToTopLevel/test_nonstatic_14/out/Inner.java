package p;

import p.A.X;

class Inner {
	/** Comment */
	private A a;

	/**
	 * @param a
	 */
	Inner(A a) {
		this.a= a;
	}

	void f(){
		X x= this.a.new X();
	}
}