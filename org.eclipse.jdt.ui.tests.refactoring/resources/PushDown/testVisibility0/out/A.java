package p;

abstract class A {
	
	protected int bar() {
		return foo();
	}

	public abstract int foo();
}
class B extends A {

	public int foo() {
		return bar();
	}
}