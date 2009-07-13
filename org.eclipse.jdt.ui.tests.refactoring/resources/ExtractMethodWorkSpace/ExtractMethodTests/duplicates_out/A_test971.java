package duplicates_out;

public class A_test971 {
	void f(){
		new Object() {
			public String toString() {
				extracted();
				return null;
			}
		};
		extracted();
	}

	protected void extracted() {
		/*[*/System.out.println("hello world");/*]*/
	}
}
