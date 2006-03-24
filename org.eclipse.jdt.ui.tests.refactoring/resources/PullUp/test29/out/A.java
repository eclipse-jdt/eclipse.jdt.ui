package p;
abstract class A{

	public abstract boolean m(int[] a) throws Exception;
}
class B extends A{
	@Override
	public boolean m(int[] a) throws Exception {
		return true;
	}
}
class C extends A{

	/* (non-Javadoc)
	 * @see p.A#m(int[])
	 */
	public boolean m(int[] a) throws Exception {
		return false;
	}
}