package p;

class A {
	A fA;
	public void m() {}
	public void m1() {}
	void f(){
		A a= fA;
		a.m1();
	}
}