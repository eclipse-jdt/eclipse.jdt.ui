package p;

class A implements I {
	public void m(){}
	public void m1(){}
	void f(){
		A a= new A();
		a.m();

		A a1= new A();
		a1.m1();
	}
}