package p;
/**
 * @see A
 * @see p.A
 * @see p.B#m()
 * @see p.B#f
 */
class B{

	/**
	 * @see #f
	 * @see B#f
	 * @see p.B#f
	 */
	public static int f;

	/**
	 * @see A
	 * @see p.A
	 * @see #m()
	 * @see B#m()
	 * @see p.B#m()
	 */
	public static void m(){
		B.m();
		m();
		f= B.f;
	}
}