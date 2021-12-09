package lambdaExpression18_in;

@FunctionalInterface
interface FI {
	int foo1(int a);
}

class FI_1 {
	void fun(int a) {
		FI i1 = x1-> x1;
		FI i2 = xxx-> /*]*/extracted(a, i1, xxx)/*[*/;
	}

	private int extracted(int a, FI i1, int xxx) {
		i1.foo1(a);
		return xxx;
	}
}