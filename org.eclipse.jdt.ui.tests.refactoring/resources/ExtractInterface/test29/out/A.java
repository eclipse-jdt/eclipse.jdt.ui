package p;

class A implements I {
	public void m() {}
	public void m1() {}
	protected I g() {
		return this;	
	}
}
class A1 extends A{
	protected A g() {
		g().m1();
		return this;	
	}
}