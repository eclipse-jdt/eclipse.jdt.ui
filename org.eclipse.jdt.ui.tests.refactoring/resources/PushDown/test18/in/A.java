package p;
abstract class A{

	public abstract void f();

	public void m() {}
}
abstract class B extends A{
}
class C extends A{
	public void f(){}
}