package p;

abstract class A {
	public abstract int m();
	protected void finalize() {
		m();
	}
}

class B extends A {
	public int m() {
		return 17;
	}
}
