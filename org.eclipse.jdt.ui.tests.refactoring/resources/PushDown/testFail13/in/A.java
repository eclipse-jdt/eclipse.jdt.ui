package p;

class A{
	A b = new A();
	public void m() {}
	public void n() { b.m(); }
}
class B extends A{
}