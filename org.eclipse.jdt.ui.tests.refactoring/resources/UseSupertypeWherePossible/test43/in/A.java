package p;

class A implements I {
	public void m() {}
	public void m1() {}
	void test(){
		A a0= new A();
		((A)a0).m();
	}
}