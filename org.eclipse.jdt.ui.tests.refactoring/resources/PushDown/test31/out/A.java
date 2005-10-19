package p;

abstract class A {
	public enum TEST {
		CHECK;
	}
	private int bar() {
		return foo();
	}

	public abstract int foo();
}
class B extends A {

	@Override
	public int foo() {
		return 2;
	}
}