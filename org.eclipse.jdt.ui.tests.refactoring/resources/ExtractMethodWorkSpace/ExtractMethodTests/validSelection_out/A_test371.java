package validSelection_out;

public class A_test371 {
	protected void foo() {
		extracted();
		// comment
	}

	protected void extracted() {
		/*[*/// comment
		foo();
		// comment
		foo();/*]*/
	}
}
