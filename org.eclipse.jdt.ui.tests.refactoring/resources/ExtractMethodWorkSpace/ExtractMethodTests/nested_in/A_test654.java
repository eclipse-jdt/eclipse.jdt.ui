package nested_in;

public class A_test654 {
	public void foo() {
		class Inner {
			public void foo() {
				/*[*/foo();/*]*/
			}
		}
	}
}
