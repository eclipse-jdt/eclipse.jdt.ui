package p;

class A implements I {
	public void m() {}
	public void m1() {}
	void g() {
		A a= (A)new A();
		((A)this).m1();
		((A)this).m();
		(((A)this)).m();
	}
}