package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo1(int s1, int s2);
}

@FunctionalInterface
interface I2 {
	I1 foo2(int n1);
}

interface I_Test {
	static I2 i2 = x1 -> (a1, b1) -> {/*]*/
		return extracted();
	/*[*/};

	static int extracted() {
		int m = 4;
		return m;
	}
}