package generics_out;

public class A_test1104 {
	public <E> void foo(E param) {
		extracted(param);
	}

	protected <E> void extracted(E param) {
		/*[*/foo(param);/*]*/
	}
}
