package varargs_out;

public class A_test1200 {
	public void foo(String... args) {
		extracted(args);
	}

	protected void extracted(String... args) {
		/*[*/foo(args);/*]*/
	}
}
