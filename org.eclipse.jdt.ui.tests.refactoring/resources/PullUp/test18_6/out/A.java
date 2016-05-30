package p;
interface A{

	void m();
}
abstract class C implements A {
	public abstract void m();
}
class B extends C{
	@Override
	public void m() {

	}
}