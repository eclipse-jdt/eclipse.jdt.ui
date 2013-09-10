package lambdaExpression18_in;

@FunctionalInterface
interface I1 {
	void foo(String s);
}

class I_Test {
	public boolean flag;

	public int foo() {
		int i = 10;
		
		I1 i1 = s -> {
		/*]*/switch (i) {
			case 1:
				if (flag)
					break;
				foo();
			case 2:
				return;
			default:
				throw new NullPointerException();
			}/*[*/
		};

		return i;
	}
}