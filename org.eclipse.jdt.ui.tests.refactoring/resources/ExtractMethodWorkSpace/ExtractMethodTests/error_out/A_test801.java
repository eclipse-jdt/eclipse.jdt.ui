package error_out;

public class A_test801 {
	public void foo() {
		List l;
		extracted(l);
	}

	protected void extracted(List l) {
		/*[*/g(l);/*]*/
	}

	public void g(List l) {
	}
}
