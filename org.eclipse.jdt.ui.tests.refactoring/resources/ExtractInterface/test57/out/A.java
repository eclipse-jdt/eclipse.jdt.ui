package p;

public class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {}
	public void m1() {}
	protected I f(){
		return this;
	}
	void test(){
		f().m();
	}
}