package generics_out;

public class A_test1105 {
	public <E> void foo(E param) {
		extracted(param);
	}

	protected <E> void extracted(E param) {
		/*[*/E local= param;
		foo(local);/*]*/
	}
}
