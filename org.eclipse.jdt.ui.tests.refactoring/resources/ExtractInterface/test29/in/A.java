package p;

class A {
	public void m() {}
	public void m1() {}
	protected A g() {
		return this;	
	}
}
class A1 extends A{
	protected A g() {
		g().m1();
		return this;	
	}
}