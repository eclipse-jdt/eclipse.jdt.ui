package p;

class A implements I {
	public void m(){}
	void f(){
		A a= new A();
		a.m();
	}
}