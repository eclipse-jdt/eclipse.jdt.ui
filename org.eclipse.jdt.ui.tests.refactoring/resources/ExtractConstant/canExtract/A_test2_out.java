//8, 16 -> 8, 27   AllowLoadtime == false
package p;
class A {
	static final int foo= 1;
	static final int bar= 2;
	private static final int CONSTANT= 1 + 3 * bar;
	static final int baz= 3;
	void f() {
		int i= CONSTANT;
	}
}