package p;

class A implements I {
	public void m(){}
	public void m1(){}
	void f(){
		I a= new A(), a1 = new A();
		a.m();

		a1.m1();
	}
}