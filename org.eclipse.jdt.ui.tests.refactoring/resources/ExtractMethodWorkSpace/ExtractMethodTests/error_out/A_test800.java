package error_out;

public class A_test800 {
	public void fails() {
		extracted();
	}
	public void foo() {
		extracted();
	}
	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
