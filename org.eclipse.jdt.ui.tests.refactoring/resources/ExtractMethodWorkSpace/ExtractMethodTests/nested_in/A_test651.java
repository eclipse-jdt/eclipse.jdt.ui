package nested_in;

public class A_test651 {
	public void foo() {
		Runnable run= new Runnable() {
			public void run() {
				/*[*/foo();/*]*/
			}
		};
	}
}
