package p;
abstract class A{

	public abstract void m();
}
class B extends A{
	@Override
	public void m() {
	}
}
class B1 extends A{
	public void m() {
	}
}
