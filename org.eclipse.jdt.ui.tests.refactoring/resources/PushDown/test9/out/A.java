package p;
abstract class A{
	/**
	 * comment
	 */
	public abstract void m();
}
class B extends A{

	@Override
	public void m() {}
}
class B1 extends B{
}
class C extends A{

	@Override
	public void m() {}
}