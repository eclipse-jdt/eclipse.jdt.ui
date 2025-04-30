package p;

class A {
	void a(A a){}
	void a(B b){}
	public void m(B b) {
		a(b);
	}
}