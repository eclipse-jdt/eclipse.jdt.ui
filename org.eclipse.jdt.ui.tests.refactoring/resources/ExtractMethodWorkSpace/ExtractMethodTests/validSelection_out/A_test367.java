package validSelection_out;

public class A_test367 {
	protected void foo() {
		extracted();
		// comment
	}

	protected void extracted() {
		/*[*/// comment
		foo();/*]*/
	}
}
