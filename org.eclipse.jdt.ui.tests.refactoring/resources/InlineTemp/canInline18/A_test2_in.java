package p;

class TestInlineLambda2 {
	private void fun1() {
		final FI[] fiArr = {x -> x++};		
		FI[] a = fiArr;
		FI[][] b = {fiArr};
		Object[] c = fiArr;
		Object[][] d = {fiArr};
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}