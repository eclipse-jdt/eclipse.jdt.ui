package p;

class A{
	B b = new B();
	public void n() { b.m(); }
}
class B extends A{

	public void m() {}
}