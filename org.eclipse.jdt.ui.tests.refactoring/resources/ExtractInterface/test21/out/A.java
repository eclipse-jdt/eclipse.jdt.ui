package p;

class A extends Exception implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	void e() throws A{}
	void g() {
		try{
			e();
		} catch (A a){
		}
	}
}