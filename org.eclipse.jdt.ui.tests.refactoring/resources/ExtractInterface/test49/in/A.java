package p;

class A {
	public void m() {}
	public void m1() {}
	void g() {
		f((A)this);
	}
	void f(A a){
		a.m1();
	}
}