//5, 23 -> 5, 34   AllowLoadtime == false
package p;
class A {
	private static final int CONSTANT= 2 * b() * 5;
	static void f() {
		int i= 2*(1 + CONSTANT);
		System.out.println(i);
		System.out.println(CONSTANT  +1);
	}
	static int b() {
		return 4;	
	}
}