package p;

class A implements I {
	public void m() {}
	void g() {
		A a= (A)new A();
	}
}