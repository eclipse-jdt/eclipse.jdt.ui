package p;

class A {
	public void m() {}
	public void m1() {}
	void f(Inter i){
		A a= new A();
		i.work(a);
	}
}