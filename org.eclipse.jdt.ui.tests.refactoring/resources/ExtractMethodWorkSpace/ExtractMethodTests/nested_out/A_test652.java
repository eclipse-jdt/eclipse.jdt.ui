package nested_out;

public class A_test652 {
	public void foo() {
		Runnable run= new Runnable() {
			public void run() {
				foo();
			}
		};
		
		extracted();
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
