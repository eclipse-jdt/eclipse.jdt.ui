package p;
abstract class A{

	protected abstract int m();
}
class B extends A{
	public int m() {
		return 2 +3;
	}
}
class C extends A{

	/**
	 * @see p.A#m(int[])
	 */
	public int m() {
		return 0;
	}
}