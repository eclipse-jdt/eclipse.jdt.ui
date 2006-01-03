package p;
class A{
	/**
	 * @deprecated Use {@link #m(boolean,int)} instead
	 */
	protected void m(int i, boolean b){
		m(b, i);
	}
	protected void m(boolean b, int i){
	}
	private void foo(){
		m(true, 2);
	}
}
class B extends A{
	/**
	 * @deprecated Use {@link #m(boolean,int)} instead
	 */
	protected void m(int j, boolean b){
		m(b, j);
	}

	protected void m(boolean b, int j){
		m(false, 6);
		super.m(true, 4);
	}
}