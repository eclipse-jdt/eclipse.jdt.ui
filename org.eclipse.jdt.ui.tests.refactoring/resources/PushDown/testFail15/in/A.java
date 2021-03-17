package p;

class A{
	public void m() {}
	public void n() { new A().m(); }
}
class B extends A{
}