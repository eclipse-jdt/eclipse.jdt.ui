package duplicates_in;

public class A_test955 {
	void foo() {
		/*[*/bar();
		bar();/*]*/
		bar();
		bar();
	}
	void bar() {
	}
}
