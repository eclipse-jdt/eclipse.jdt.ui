//8, 16 -> 8, 22   AllowLoadtime == true
package p;

class S {
	static int s;

	int f() {
		return 23 * s;
	}
}