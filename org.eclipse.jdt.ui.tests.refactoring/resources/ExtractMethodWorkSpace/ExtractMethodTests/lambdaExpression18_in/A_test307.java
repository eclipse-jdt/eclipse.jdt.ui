package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

class X {
	I1 i1= (a) -> {
		/*[*/int b = a;/*]*/
		return a + b;
	};
}