package p;

class A implements I {
	public void m() {}
	public void m1() {}
	A g() {
		A a= new A();
		g().m1();
		return a;
	}
}