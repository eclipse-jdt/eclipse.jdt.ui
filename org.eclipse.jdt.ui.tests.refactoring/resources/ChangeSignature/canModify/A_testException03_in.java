package p;

abstract class A {
	public abstract void m() throws RuntimeException;
}

class B extends A {
	public int m() throws Illegal, RuntimeException {
	}
}