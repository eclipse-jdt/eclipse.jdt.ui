// 5, 30 -> 5, 35  replaceAll = true, removeDeclaration = true
package p;

class TestInlineMethodRefArray {
	public static final FI[] fiArr = { TestInlineMethodRefArray::m };

	private static int m(int x) {
		return x++;
	}

	FI[] a = fiArr;
	FI[][] b = { fiArr };
	Object[] c = fiArr;
	Object[][] d = { fiArr };
}

@FunctionalInterface
interface FI {
	int foo(int x);
}