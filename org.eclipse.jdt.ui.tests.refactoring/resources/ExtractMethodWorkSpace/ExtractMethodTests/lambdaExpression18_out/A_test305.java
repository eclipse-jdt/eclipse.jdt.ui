package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

interface I2 {
	I1 i1 = (a) -> {
		int b = extracted();
		return a + b;
	};

	static int extracted() {
		/*[*/int b = 10;/*]*/
		return b;
	}
}