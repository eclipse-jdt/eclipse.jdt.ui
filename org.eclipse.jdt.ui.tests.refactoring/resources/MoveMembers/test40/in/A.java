package p;
/**
 * @see p.A
 * @see #m()
 * @see A#m()
 * @see p.A#m()
 * @see #f
 * @see A#f
 * @see p.A#f
 */
public class A{
	public A() {
		m();
		f= A.f;
	}
	
	/**
	 * @see A
	 * @see p.A
	 * @see #m()
	 * @see A#m()
	 * @see p.A#m()
	 */
	public static void m(){
		A.m();
		m();
		f= A.f;
	}
	
	/**
	 * @see #f
	 * @see A#f
	 * @see p.A#f
	 */
	public static int f;
}