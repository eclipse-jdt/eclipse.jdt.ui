package p;

abstract class A {
	
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