package p;
class A{
	/**
	 * @deprecated Use {@link #m(int,int)} instead
	 */
	private void m(int i){
		m(0, i);
	}

	private void m(int x, int i){
		m(x, i);
	}
}