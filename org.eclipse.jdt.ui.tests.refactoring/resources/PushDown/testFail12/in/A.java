//pushing bar not possible - referenced by foo
package p;

class A {
	
	private int bar= foo();

	private int foo() {
		return bar;
	}
}
class B extends A {
}