package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void f(Inter i){
		I a= new A();
		i.work(a);
	}
}