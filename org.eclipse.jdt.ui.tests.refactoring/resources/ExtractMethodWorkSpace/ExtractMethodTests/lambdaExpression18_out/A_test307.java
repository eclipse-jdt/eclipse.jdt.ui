package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

class X {
	I1 i1= (a) -> {
		int b = extracted(a);
		return a + b;
	};

	private int extracted(int a) {
		/*[*/int b = a;/*]*/
		return b;
	}
}