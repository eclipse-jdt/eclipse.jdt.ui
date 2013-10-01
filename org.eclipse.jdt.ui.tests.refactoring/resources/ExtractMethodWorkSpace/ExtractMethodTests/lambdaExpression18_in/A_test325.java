package lambdaExpression18_in;

@FunctionalInterface
interface FI {
	int foo1(int a);
}

class FI_1 {
	void fun(int a) {
		FI i1 = x1-> x1;
		FI i2 = xxx-> {
			i1.foo1(a);
			/*]*/return xxx;/*[*/
		};
	}
}