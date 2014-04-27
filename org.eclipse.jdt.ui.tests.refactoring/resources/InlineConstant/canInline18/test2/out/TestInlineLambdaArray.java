// 5, 30 -> 5, 35  replaceAll = true, removeDeclaration = true
package p;

class TestInlineLambdaArray {
	FI[] a = new FI[]{ x -> x++ };
	FI[][] b = { new FI[]{ x -> x++ } };
	Object[] c = new FI[]{ x -> x++ };
	Object[][] d = { new FI[]{ x -> x++ } };
}

@FunctionalInterface
interface FI {
	int foo(int x);
}