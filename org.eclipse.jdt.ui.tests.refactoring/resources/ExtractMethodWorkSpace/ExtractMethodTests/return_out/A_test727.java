package return_out;

public class A_test727 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/if (true) return;
		bar();/*]*/
	}

	public void bar() {
	}
}