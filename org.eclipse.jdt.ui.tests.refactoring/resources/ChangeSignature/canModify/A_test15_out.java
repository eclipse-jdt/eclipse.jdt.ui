package p;
class A{
	/**
	 * @deprecated Use {@link #m(boolean,int)} instead
	 */
	private void m(int i, boolean b){
		m(b, i);
	}
	private void m(boolean b, int i){
	}
	private void foo(){
		m(true, 2);
	}
}