package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	void g() {
		f((I)this);
	}
	I f(I a){
		f(a).m();
		return a;
	}
}