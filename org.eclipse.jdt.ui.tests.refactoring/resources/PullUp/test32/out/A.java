package p;
abstract class A{

	public abstract int m();
}
class B extends A{
	public int m() {
		return 2 +3;
	}
}
class C extends A{

	/**
	 * @see p.A#m()
	 */
	public int m() {
		return 0;
	}
}