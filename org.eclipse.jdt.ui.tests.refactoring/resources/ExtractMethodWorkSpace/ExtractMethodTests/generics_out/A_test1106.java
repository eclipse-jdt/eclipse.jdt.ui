package generics_out;

public class A_test1106<E> {
	public void foo(E param) {
		extracted(param);
	}

	protected void extracted(E param) {
		/*[*/E local= param;
		foo(local);/*]*/
	}
}
