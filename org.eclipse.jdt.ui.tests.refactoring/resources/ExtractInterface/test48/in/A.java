package p;

class A {
	public void m() {}
	public void m1() {}
	A f(A a){
		f(a).m1();
		return a;
	}
}