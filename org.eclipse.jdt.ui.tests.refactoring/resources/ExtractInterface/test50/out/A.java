package p;

class A implements I {
	A a;
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void f(){
		a.m1();
	}
}