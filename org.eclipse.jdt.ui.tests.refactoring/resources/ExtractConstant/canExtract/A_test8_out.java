//8, 16 -> 8, 22   AllowLoadtime == true
package p;

class S {
	static int s;
	private static final int CONSTANT= 23 * s;

	int f() {
		return CONSTANT;
	}
}