package p;

class A {
	void a(A a){}

	protected void m() {
		a(this);
	}
}
