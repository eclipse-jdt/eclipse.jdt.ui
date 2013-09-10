package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	int foo(int a);
}

class X {
	void bar() {
		Runnable r = new Runnable() {
			I1 i1 = (int a) -> {
				int b = extracted();
				return a + b;
			};

			private int extracted() {
				/*[*/int b = 10;/*]*/
				return b;
			}
			
			@Override
			public void run() {
			}
		};
	}
}