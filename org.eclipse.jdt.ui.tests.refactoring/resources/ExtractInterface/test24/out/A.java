package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void g() {
		I a= (I)new A();
		((A)this).m1();
		((I)this).m();
		(((I)this)).m();
	}
}