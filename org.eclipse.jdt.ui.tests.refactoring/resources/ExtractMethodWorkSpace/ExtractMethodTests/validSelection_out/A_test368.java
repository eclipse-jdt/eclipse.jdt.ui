package validSelection_out;

public class A_test368 {
	protected void foo() {
		// comment
		extracted();
	}

	protected void extracted() {
		/*[*/foo();
		/* comment*/ /*]*/
	}
}
