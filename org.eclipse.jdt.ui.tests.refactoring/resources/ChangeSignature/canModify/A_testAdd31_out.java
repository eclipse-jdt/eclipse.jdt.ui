package p;
class A{
	/**
	 * @deprecated Use {@link #m(int,int)} instead
	 */
	void m(int i){
		m(i, 0);
	}

	void m(int i, int x){
		m(i, x);
	}
}
class B extends A{
	/**
	 * @deprecated Use {@link #m(int,int)} instead
	 */
	void m(int j){
		m(j, 0);
	}

	void m(int j, int x){
		super.m(j, x);
		this.m(j, x);
		m(j, x);
	}
}