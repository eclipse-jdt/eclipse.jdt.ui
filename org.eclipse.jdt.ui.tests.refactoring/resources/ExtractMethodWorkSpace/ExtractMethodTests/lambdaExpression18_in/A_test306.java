package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

interface I2 {
	public default int foo() {
		I1 i1 = (a) -> {
			/*[*/int b = a;/*]*/
			return a + b;
		};		
	}
}