package p;

class A {
	public void m() {}
	public void m1() {}
	void test(){
		A a1;
		a1= null;
		a1.m1();
	}
}