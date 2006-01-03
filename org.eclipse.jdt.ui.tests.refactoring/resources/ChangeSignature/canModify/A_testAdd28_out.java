package p;
class A{
	/**
	 * @deprecated Use {@link #m(int,int)} instead
	 */
	private void m(int i){
		m(i, 0);
	}

	private void m(int i, int x){
	}
}