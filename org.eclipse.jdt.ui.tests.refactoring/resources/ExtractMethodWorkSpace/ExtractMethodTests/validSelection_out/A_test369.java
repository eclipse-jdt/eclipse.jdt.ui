package validSelection_out;

public class A_test369 {
	protected void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/// comment
		foo();
		/* comment*/ /*]*/
	}
}
