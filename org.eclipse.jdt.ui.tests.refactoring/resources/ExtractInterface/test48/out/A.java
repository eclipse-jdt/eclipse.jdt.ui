package p;

class A implements I {
	public void m() {}
	public void m1() {}
	A f(A a){
		f(a).m1();
		return a;
	}
}