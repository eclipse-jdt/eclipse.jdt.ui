package p;

class A implements I {
	public void m(){}
	void f(){
		I a= new A();
		a.m();
	}
}