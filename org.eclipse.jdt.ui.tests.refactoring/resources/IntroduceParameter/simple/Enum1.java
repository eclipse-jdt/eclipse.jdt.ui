//selection: 7, 18, 7, 24
//name: enum1 -> a
package simple;

public class C1 {
	public static void test() {
		new Test(Enum.A);
	}
}
enum Enum {
	A
}
class Test {
	public Test(Enum e) {
	}
}