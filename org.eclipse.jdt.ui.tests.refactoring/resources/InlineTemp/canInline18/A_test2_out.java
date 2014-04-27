package p;

class TestInlineLambda2 {
	private void fun1() {
		FI[] a = new FI[]{x -> x++};
		FI[][] b = {new FI[]{x -> x++}};
		Object[] c = new FI[]{x -> x++};
		Object[][] d = {new FI[]{x -> x++}};
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}