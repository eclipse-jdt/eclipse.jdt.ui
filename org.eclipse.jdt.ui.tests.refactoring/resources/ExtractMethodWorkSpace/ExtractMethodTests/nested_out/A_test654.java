package nested_out;

public class A_test654 {
	public void foo() {
		class Inner {
			public void foo() {
				extracted();
			}

			protected void extracted() {
				/*[*/foo();/*]*/
			}
		}
	}
}
