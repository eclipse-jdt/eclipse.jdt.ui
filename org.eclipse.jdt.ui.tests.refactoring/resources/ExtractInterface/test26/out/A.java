package p;

class A implements I {
	public void m() {}
	public void m1() {}
	protected I g() {
		g().m();
		return this;	
	}
}