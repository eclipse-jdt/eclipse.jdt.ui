package p;

class A{
	/**
	 * @deprecated Use {@link #m(int,boolean)} instead
	 */
	private void m(boolean y, int a){
		m(a, y);
	}

	private void m(int a, boolean yyy){
        m(a, yyy);
    }
}