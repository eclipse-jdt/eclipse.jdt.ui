package validSelection_out;

public class A_test372 {
	protected void foo() {
		// comment
		extracted();
	}

	protected void extracted() {
		/*[*/foo();
		// comment
		foo();
		// comment
		/*]*/
	}
}
