//pushing foo not possible - referenced by bar
package p;

class A {
	
	private int bar() {
		return foo();
	}

	private int foo() {
		return bar();
	}
}
class B extends A {
}