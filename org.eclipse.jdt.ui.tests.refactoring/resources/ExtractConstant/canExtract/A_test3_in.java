//8, 16 -> 8, 27   AllowLoadtime == false
package p;
class A {
	static final int foo= 1;
	static final int bar= 2;
	static final int baz= foo * (1+3*bar);
	void f() {
		int i= 1 + 3 * bar;
	}
}