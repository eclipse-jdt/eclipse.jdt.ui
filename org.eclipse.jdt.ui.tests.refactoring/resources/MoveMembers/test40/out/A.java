package p;
/**
 * @see p.A
 * @see B#m()
 * @see B#m()
 * @see p.B#m()
 * @see B#f
 * @see B#f
 * @see p.B#f
 */
public class A{
	public A() {
		B.m();
		B.f= B.f;
	}
}