package p;
class A{
	/**
	 * @deprecated Use {@link #m(int)} instead
	 */
	void m(int i, int j){
		m(j);
	}

	void m(int j){
		m(1);
	}
}