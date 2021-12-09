package lambdaExpression18_in;

@FunctionalInterface
interface FI {
	void foo1(int a);
}

class FI_1 {
	void foo(int a, FI fi) {
		fi.foo1(a);
	}
	void fun(int a) {
		foo(a, xxx -> /*]*/extracted(a)/*[*/);
	}
	private void extracted(int a) {
		System.out.println(a);
	}
}