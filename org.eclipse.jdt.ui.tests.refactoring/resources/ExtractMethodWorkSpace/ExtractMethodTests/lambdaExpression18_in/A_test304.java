package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

class X {
	void bar() {
		Runnable r = new Runnable() {
			I1 i1 = (int a) -> {
				/*[*/int b = 10;/*]*/
				return a + b;
			};
			
			@Override
			public void run() {
			}
		};
	}
}