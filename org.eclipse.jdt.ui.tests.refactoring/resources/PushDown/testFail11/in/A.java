//pushing foo not possible - referenced by bar
package p;

class A {
	
	private int bar= foo();

	private int foo() {
		return 1;
	}
}
class B extends A {
}