package p;
abstract class A{

	public abstract void f();

	public void m() {
		f();
	}
}
class B extends A{
	@Override
	public void f(){
	}
}