package p;

class A {
	
	private int bar() {
		return foo();
	}

	public int foo() {
		return 2;
	}
}
class B extends A {
}