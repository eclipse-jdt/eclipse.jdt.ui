package nested_out;

public class A_test652 {
	public void foo() {
		Runnable run= new Runnable() {
			public void run() {
				extracted();
			}
		};
		
		extracted();
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
