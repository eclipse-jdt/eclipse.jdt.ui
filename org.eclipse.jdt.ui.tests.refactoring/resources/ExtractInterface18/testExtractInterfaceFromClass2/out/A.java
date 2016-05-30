package p;

class A extends C implements I {
	/* (non-Javadoc)
	 * @see p.I#m()
	 */
	@Override
	public void m() {
	}
}

abstract class C {
	abstract void m();
}
