package duplicates_out;

public class A_test952 {
	void foo() {
		extracted();
		extracted();
	}

	protected void extracted() {
		/*[*/bar();/*]*/
	}

	void bar() {
	}
}
