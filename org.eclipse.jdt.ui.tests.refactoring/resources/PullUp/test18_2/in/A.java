package p;
interface A{
}
abstract class C implements A {
	public abstract void m();
}
class B extends C{
	@Override
	public void m() {

	}
}