package p;

class A implements I {
	A fA;
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void f(){
		A a= fA;
		a.m1();
	}
}