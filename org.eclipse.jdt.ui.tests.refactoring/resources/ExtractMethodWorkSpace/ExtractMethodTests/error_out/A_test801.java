package error_out;

public class A_test801 {
	public void foo() {
		List l;
		extracted();
	}

	protected void extracted() {
		/*[*/g(l);/*]*/
	}

	public void g(List l) {
	}
}
