//selection: 7, 18, 7, 24
//name: enum1 -> a
package p;

public class C1 {
	public static void test(Enum a) {
		new Test(a);
	}
}
enum Enum {
	A
}
class Test {
	public Test(Enum e) {
	}
}