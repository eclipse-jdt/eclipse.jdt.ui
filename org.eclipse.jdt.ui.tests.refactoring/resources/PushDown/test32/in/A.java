package p;

class A {
	@interface Annotation {
		String name();
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