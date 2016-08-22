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
	@Override
	public int m() {
		// TODO Auto-generated method stub
		return 0;
	}
}