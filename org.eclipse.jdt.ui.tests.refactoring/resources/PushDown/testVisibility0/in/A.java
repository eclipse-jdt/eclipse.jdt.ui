package p;

class A {
	
	private int bar() {
		return foo();
	}

	public int foo() {
		return bar();
	}
}
class B extends A {
}