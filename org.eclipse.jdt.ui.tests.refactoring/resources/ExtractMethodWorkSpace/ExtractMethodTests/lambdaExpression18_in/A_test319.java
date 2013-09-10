package lambdaExpression18_in;

@FunctionalInterface
interface F {
	String foo();
}

interface I_Test {
	F f_11= () -> /*[*/ "abc"; /*]*/
}