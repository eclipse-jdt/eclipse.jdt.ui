package p;
class A{
	/**
	 * @deprecated Use {@link #m(int)} instead
	 */
	private void m(){
		m(0);
	}

	private void m(int x){
		m(x);
	}
}