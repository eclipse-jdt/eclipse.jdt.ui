package p;
abstract class A{
}
abstract class B extends A{

	public abstract void f();

	public void m() {}
}
class C extends A{
	public void f(){}

	public void m() {}
}