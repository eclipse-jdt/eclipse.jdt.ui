package p;

class A {
	private TEST bar() {
		return foo();
	}

	public TEST foo() {
		return TEST.CHECK;
	}
}
class B extends A {
}
enum TEST {
	CHECK;
}