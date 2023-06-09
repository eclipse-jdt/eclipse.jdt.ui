package p;

class A {
	void a(A a){}
	public void a(B b){}
	protected void m(B b) {
		foo2(b);
		a(this);
	}
	protected void foo2(B b) {
		m(b);
	}
}
