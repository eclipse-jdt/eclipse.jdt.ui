package p;

class A {
	public void m() {}
	public void m1() {}
	void test(){
		A a0= new A();
		((A)a0).m();
	}
}