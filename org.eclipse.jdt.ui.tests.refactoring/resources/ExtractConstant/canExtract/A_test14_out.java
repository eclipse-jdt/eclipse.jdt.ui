//10, 28 -> 10, 44   AllowLoadtime == true
package p;

class S {
	public static S instance= new S();

	private static final int CONSTANT= S.instance.s + 1;

	int s;

	int f() {
		System.out.println(CONSTANT);
		return 1;
	}
}