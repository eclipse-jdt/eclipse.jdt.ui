//5, 23 -> 5, 34   AllowLoadtime == false
package p;
class A {
	static void f() {
		int i= 2*(1 + 2 * b() * 5);
		System.out.println(i);
		System.out.println(2*b     ()*5  +1);
	}
	static int b() {
		return 4;	
	}
}