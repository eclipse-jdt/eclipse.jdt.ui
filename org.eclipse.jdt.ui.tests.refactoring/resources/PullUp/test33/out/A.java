package p;
abstract class A{

	public abstract int m();
}
abstract class B extends A{
}
class C extends A{

	/* (non-Javadoc)
	 * @see p.A#m()
	 */
	public int m() {
		return 0;
	}
}