package p;

class A {
	public void m() {}
	public void m1() {}
	void g() {
		f((A)this);
	}
	A f(A a){
		f(a).m();
		return a;
	}
}