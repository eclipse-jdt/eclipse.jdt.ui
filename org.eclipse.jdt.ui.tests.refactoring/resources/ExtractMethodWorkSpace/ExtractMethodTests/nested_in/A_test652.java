package nested_in;

public class A_test652 {
	public void foo() {
		Runnable run= new Runnable() {
			public void run() {
				foo();
			}
		};
		
		/*[*/foo();/*]*/
	}
}
