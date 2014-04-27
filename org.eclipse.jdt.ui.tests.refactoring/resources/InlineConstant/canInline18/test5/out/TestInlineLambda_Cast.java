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
	{
		fun1((ISup) (FSub) x -> x++);
	}

	private void fun1(ISup sup) { }
}