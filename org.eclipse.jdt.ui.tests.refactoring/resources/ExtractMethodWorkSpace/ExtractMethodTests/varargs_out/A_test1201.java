package varargs_out;

public class A_test1201 {
	public void foo(String... args) {
		extracted(args);
	}

	protected void extracted(String... args) {
		/*[*/System.out.println(args[1]);
		System.out.println(args[2]);/*]*/
	}
}
