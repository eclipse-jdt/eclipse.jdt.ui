package p;
abstract class A{

	public abstract boolean m(int[] a) throws Exception;
}
class B extends A{
	public boolean m(int[] a) throws Exception {
		return true;
	}
}
class B1 extends B{
}
abstract class C extends A{
}
class D extends C{

	/**
	 * @see p.A#m(int[])
	 */
	public boolean m(int[] a) throws Exception {
		return false;
	}
}
class D1 extends C{

	/**
	 * @see p.A#m(int[])
	 */
	public boolean m(int[] a) throws Exception {
		return false;
	}
}
class E extends D{
}