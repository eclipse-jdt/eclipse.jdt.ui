package duplicates_in;

public class A_test964 {
	void test() {
		new Object() {
			public void yes() {
				yes();
				System.out.println("hello world");
			}
		};
		/*[*/System.out.println("hello world");/*]*/
	}
}