package p;

class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	public static int s= 0;
	void t(){
		A.s= 1;
	}
}