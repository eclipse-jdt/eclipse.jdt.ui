package duplicates_out;

public class A_test963 {
	void test() {
		new Object() {
			public void yes() {
				extracted();
			}

			protected void extracted() {
				/*[*/System.out.println("hello world");/*]*/
			}
		};
		System.out.println("hello world");
	}
}