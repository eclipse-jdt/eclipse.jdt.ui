package lambdaExpression18_in;

@FunctionalInterface
interface F {
	int foo(int s1, int s2);
}

interface I_Test {
	F f_1= (/*]*/int n1, int n2/*[*/) -> n1 * n2;
}