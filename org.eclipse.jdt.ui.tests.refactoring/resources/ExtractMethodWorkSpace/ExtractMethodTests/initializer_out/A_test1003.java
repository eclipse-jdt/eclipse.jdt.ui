package initializer_out;

public class A_test1003 {
	static {
		int i= extracted();
	}

	protected static int extracted() {
		return /*[*/bar()/*]*/;
	}

	private static int bar() {
		return 10;
	}
}
