package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

class X {
	void foo() {
		I1 i1 = (int a) -> {
			int b = extracted();
			return a + b;
		};
	}

	private int extracted() {
		/*[*/int b = 10;/*]*/
		return b;
	}
}