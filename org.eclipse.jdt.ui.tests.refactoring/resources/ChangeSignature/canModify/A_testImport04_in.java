package p;

abstract class A {
	public abstract int m();
	protected void finalize() {
		m();
	}
}
