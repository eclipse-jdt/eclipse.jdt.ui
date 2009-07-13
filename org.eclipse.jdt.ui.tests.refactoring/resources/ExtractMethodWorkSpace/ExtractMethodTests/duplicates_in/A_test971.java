package duplicates_in;

public class A_test971 {
	void f(){
		new Object() {
			public String toString() {
				/*[*/System.out.println("hello world");/*]*/
				return null;
			}
		};
		System.out.println("hello world");
	}
}
