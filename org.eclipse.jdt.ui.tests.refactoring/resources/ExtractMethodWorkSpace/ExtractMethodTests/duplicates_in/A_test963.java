package duplicates_in;

public class A_test963 {
	void test() {
		new Object() {
			public void yes() {
				/*[*/System.out.println("hello world");/*]*/
			}
		};
		System.out.println("hello world");
	}
}