package p;

class A {
	public void m() {}
	public void m1() {}
	void f(){
		A a= create();
		a.m1();
	}
	A create(){
		return null;
	}
}