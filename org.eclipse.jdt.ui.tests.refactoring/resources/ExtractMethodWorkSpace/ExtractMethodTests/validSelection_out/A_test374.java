package validSelection_out;

public class A_test374 {
	protected void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/// comment
		foo();
		// comment
		foo();
		// comment/*]*/
	}
}
