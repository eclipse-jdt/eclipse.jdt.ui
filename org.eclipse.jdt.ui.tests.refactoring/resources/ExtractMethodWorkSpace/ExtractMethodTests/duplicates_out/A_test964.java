package duplicates_out;

public class A_test964 {
	void test() {
		new Object() {
			public void yes() {
				yes();
				extracted();
			}
		};
		extracted();
	}

	protected void extracted() {
		/*[*/System.out.println("hello world");/*]*/
	}
}