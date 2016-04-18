package p;
interface A{

	void m();
}
abstract class C implements A {
	public abstract void m();
}
class B extends C{
	class Override {
		// used to force @java.lang.Override instead of @Override
	}
	@java.lang.Override
	public void m() {

	}
}