package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m(){}
	/* (non-Javadoc)
	 * @see p.I#m1()
	 */
	public void m1(){}
	void f(){
		I a= new A();
		a.m();
	}
}