package p;
abstract class A{

	public abstract void f();

	public abstract void m();
}
abstract class B extends A{

	public void m() {}

	public abstract void f();
}
class C extends A{
	public void f(){}

	public void m() {}
}