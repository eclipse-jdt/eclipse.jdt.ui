package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

interface I2 {
	I1 i1 = (a) -> {
		/*[*/int b = 10;/*]*/
		return a + b;
	};
}