package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void f(){
		A a= create();
		a.m1();
	}
	A create(){
		return null;
	}
}