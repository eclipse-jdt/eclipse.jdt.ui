package p;

abstract class A {
	@interface Annotation {
		String name();
	}
	private int bar() {
		return foo();
	}

	public abstract int foo();
}
class B extends A {

	public int foo() {
		return 2;
	}
}