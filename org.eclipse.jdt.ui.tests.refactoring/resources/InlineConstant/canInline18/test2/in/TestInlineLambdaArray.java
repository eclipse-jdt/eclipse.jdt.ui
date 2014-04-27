// 5, 30 -> 5, 35  replaceAll = true, removeDeclaration = true
package p;

class TestInlineLambdaArray {
	public static final FI[] fiArr = { x -> x++ };
	FI[] a = fiArr;
	FI[][] b = { fiArr };
	Object[] c = fiArr;
	Object[][] d = { fiArr };
}

@FunctionalInterface
interface FI {
	int foo(int x);
}