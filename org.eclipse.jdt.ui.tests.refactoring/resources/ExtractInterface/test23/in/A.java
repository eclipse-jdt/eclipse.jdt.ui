package p;

class A {
	public void m() {}
	void g() {
		A a= (A)new A();
	}
}