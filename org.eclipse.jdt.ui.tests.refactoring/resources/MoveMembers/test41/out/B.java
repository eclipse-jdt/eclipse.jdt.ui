package p;
/**
 * @see A
 * @see p.A
 * @see p.B#m()
 * @see p.A#f
 */
class B{

	/**
	 * @see A
	 * @see p.A
	 * @see #m()
	 * @see B#m()
	 * @see p.B#m()
	 * @see A#f
	 * @see A#f
	 * @see p.A#f
	 */
	public static void m(){
		B.m();
		m();
		A.f= A.f;
	}
}