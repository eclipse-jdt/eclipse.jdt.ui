package p;

class A {
	public void m() {}
	public void m1() {}
	void test(){
		A a0= new A();
		f(a0);
	}
	void f(A a){
		a.m1();
	}
}