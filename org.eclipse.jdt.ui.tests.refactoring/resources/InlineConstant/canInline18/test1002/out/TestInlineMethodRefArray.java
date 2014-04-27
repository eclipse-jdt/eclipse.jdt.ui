// 5, 30 -> 5, 35  replaceAll = true, removeDeclaration = true
package p;

class TestInlineMethodRefArray {
	private static int m(int x) {
		return x++;
	}

	FI[] a = new FI[]{ TestInlineMethodRefArray::m };
	FI[][] b = { new FI[]{ TestInlineMethodRefArray::m } };
	Object[] c = new FI[]{ TestInlineMethodRefArray::m };
	Object[][] d = { new FI[]{ TestInlineMethodRefArray::m } };
}

@FunctionalInterface
interface FI {
	int foo(int x);
}