package p;

class A implements I {
	public void m() {}
	public void m1() {}
	void test(){
		I a0= new A();
		f(a0);
	}
	void f(I a){
		a.m();
	}
}