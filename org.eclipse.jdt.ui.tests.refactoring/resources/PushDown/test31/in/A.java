package p;

class A {
	public enum TEST {
		CHECK;
	}
	private int bar() {
		return foo();
	}

	public int foo() {
		return 2;
	}
}
class B extends A {
}