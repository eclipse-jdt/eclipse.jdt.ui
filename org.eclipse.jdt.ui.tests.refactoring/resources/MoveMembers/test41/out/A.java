package p;
/**
 * @see p.A
 * @see B#m()
 * @see B#m()
 * @see p.B#m()
 * @see #f
 * @see A#f
 * @see p.A#f
 */
public class A{
	public A() {
		B.m();
		f= A.f;
	}
	
	/**
	 * @see #f
	 * @see A#f
	 * @see p.A#f
	 */
	public static int f;
}