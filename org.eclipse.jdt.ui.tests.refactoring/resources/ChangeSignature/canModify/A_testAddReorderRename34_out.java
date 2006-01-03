package p;
class A{
	/**
	 * @deprecated Use {@link #m(boolean,Object,int)} instead
	 */
	private int m(int iii, boolean j){
		return m(j, null, iii);
	}

	private int m(boolean jj, Object x, int i){
		return m(false, x, m(jj, x, i));
	}
}