package p;

class A {
	/**
	 * @deprecated Use {@link #m(boolean,int)} instead
	 */
	public void m(int i, final boolean b){
		m(b, i);
	}

	public void m(final boolean b, int i){}
}
