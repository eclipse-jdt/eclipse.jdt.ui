package p;

public class X implements I {
	public double x;
	/* (non-Javadoc)
	 * @see p.I#dot(p.X)
	 */
	public final double dot(X v) {
		return v.x;
	}
}
