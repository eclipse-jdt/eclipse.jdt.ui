package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m(){}
	void f(){
		I a= new A();
		a.m();
	}
}