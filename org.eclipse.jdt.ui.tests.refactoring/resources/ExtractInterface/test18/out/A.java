package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m(){}
	public void m1(){}
	void f(){
		I a= new A();
		a.m();

		A a1= new A();
		a1.m1();
	}
}