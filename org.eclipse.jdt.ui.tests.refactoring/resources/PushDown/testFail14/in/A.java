package p;

class A{
	public void m() {}
	public void n() { this.m(); }
}
class B extends A{
}