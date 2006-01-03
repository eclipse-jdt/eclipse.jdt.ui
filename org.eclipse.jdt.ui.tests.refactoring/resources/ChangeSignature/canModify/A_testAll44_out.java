package p;
class A{
	boolean m(int j){
		m(1);
	}

	/**
	 * @deprecated use instead m(int j)
	 */
	public void m(int i, int j) {
		m(j);
	}
}