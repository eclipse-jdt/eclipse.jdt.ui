package initializer_in;

public class A_test1003 {
	static {
		int i= /*[*/bar()/*]*/;
	}

	private static int bar() {
		return 10;
	}
}
