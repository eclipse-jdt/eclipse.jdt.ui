//10, 28 -> 10, 44   AllowLoadtime == true
package p;

class S {
	public static S instance= new S();

	int s;

	int f() {
		System.out.println(S.instance.s + 1);
		return 1;
	}
}