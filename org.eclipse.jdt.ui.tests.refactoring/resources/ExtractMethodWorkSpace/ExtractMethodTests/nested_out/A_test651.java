package nested_out;

public class A_test651 {
	public void foo() {
		Runnable run= new Runnable() {
			public void run() {
				extracted();
			}

			protected void extracted() {
				/*[*/foo();/*]*/
			}
		};
	}
}
