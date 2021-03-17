package p;

class A{
	public void n() { new B().m(); }
}
class B extends A{

	public void m() {}
}