package p;

abstract class A {
	public abstract void m();
}

class B extends A {
	public int m() throws Illegal {
	}
}