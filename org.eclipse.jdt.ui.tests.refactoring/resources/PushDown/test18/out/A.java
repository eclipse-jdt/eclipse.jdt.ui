package p;
abstract class A{
}
abstract class B extends A{

	public void m() {}

	public abstract void f();
}
class C extends A{
	public void f(){}

	public void m() {}
}