package p;

class A implements I {
	public void m() {}
	public void m1() {}
	void test(){
		A a0= new A();
		A a1= a0;
		A a2= a1;
		a2.m1();
	}
}