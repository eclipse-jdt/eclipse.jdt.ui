package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m(){}
	public void m1(){}
	void f(){
		A a= new A(), a1 = new A();
		a.m();

		a1.m1();
	}
}