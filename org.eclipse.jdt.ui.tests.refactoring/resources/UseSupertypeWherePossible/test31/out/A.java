package p;

class A implements I {
	public void m() {}
	public void m1() {}
	void g() {
		f((I)this);
	}
	I f(I a){
		f(a).m();
		return a;
	}
}