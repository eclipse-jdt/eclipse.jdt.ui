package error_out;

public class A_test800 {
	public void fails() {
		foo()
	}
	public void foo() {
		extracted();
	}
	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
