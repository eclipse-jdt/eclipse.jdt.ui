package p;

class A implements I {
	int f;
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	protected A f(){
		return this;
	}
	void test(){
		f().f=0;
	}
}