package p;

abstract class A {
	public abstract int m(Object o);
	protected void finalize() {
		m(null);
	}
}
