//5, 16 -> 5, 17   AllowLoadtime == false
package p;
class A {
	private static final int CONSTANT= 0;

	void f() {
		int i= CONSTANT;
	}
}