package validSelection_out;

public class A_test365 {
	public void bar() throws NullPointerException {
	}
	
	protected void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/bar();/*]*/
	}
}
