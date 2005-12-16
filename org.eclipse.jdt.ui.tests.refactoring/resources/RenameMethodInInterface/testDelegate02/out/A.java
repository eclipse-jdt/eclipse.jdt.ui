package p;

public interface I {
	public void k();

	/**
	 * @deprecated Use {@link #k()} instead
	 */
	public void m();
}

interface B extends I {
}

interface C extends B {
	public void k();

	/**
	 * @deprecated Use {@link #k()} instead
	 */
	public void m();
}
