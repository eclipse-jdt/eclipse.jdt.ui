package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void test(){
		I a0= new A();
		I a1;
		a1= a0;
		a1.m();
	}
}