package duplicates_out;

public class A_test955 {
	void foo() {
		extracted();
		extracted();
	}
	protected void extracted() {
		/*[*/bar();
		bar();/*]*/
	}
	void bar() {
	}
}
