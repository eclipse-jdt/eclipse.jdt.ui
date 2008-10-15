package p;

public class A implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	public void m() {
		for (A a : getCollection()) {
			a.abc();
		}
		for (I a : getCollection()) {

		}
	}

	private void abc() {
	}
}
