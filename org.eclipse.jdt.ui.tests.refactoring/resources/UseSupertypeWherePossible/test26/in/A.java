package p;

class A implements I {
	public void m() {}
	public void m1() {}
	protected A g() {
		g().m();
		return this;	
	}
}