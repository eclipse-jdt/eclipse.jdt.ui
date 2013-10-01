package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

interface I2 {
	public default int foo() {
		I1 i1 = (a) -> {
			int b = extracted(a);
			return a + b;
		};		
	}

	public default int extracted(int a) {
		/*[*/int b = a;/*]*/
		return b;
	}
}