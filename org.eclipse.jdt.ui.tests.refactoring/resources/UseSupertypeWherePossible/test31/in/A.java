package p;

class A implements I {
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