package enums_out;

public enum A_test1151 {
	A;
	
	static {
		extracted();
	}

	protected static void extracted() {
		/*[*/foo();/*]*/
	}
	
	private static void foo() {
	}
}
