package validSelection_out;

public class A_test370 {
	protected void foo() {
		// comment
		extracted();
		// comment
	}

	protected void extracted() {
		/*[*/foo();
		// comment
		foo();/*]*/
	}
}
