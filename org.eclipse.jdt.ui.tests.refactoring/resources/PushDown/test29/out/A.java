package p;

abstract class A {
	private TEST bar() {
		return foo();
	}

	public abstract TEST foo();
}
class B extends A {

	@Override
	public TEST foo() {
		return TEST.CHECK;
	}
}
enum TEST {
	CHECK;
}