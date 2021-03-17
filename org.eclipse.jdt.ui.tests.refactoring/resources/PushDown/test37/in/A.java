package p;

class A{
	public void m() {}
	public void n() { new B().m(); }
}
class B extends A{
}