package validSelection_out;

public class A_test367 {
	protected void foo() {
		// comment
		extracted();
	}

	protected void extracted() {
		/*[*/foo();
		// comment
		foo();/*]*/
	}
}
