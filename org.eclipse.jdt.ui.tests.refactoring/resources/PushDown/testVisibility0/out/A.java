package p;

abstract class A {
	
	protected int bar() {
		return foo();
	}

	public abstract int foo();
}
class B extends A {

	@Override
	public int foo() {
		return bar();
	}
}