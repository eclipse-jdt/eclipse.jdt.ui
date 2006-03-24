package p;
abstract class A{

	public abstract int[] m();
}
class B extends A{
	@Override
	public int[] m() {
		return new int[0];
	}
}
class C extends A{

	/* (non-Javadoc)
	 * @see p.A#m()
	 */
	public int[] m() {
		return null;
	}
}