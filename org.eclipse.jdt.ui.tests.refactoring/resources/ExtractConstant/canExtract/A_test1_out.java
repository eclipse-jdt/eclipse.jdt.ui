//5, 16 -> 5, 21   AllowLoadtime == false
package p;
class A {
	private static final int CONSTANT= 1 + 2;

	void f() {
		int i= CONSTANT;
	}
}