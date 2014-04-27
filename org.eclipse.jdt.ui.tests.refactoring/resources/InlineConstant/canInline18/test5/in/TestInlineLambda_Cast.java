// 15, 30 -> 15, 36  replaceAll = true, removeDeclaration = true
package p;

interface ISup {
	int foo(double x);
	int foo(float x);
}

@FunctionalInterface
interface FSub {
	int foo(int x);
}

class TestInlineLambda1 {
	public static final FSub fi_sub = x -> x++;

	{
		fun1((ISup) fi_sub);
	}

	private void fun1(ISup sup) { }
}