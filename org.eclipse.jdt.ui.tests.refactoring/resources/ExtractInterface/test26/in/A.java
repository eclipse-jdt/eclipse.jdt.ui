package p;

class A {
	public void m() {}
	public void m1() {}
	protected A g() {
		g().m();
		return this;	
	}
}