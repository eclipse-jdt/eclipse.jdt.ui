package nested_out;

public class A_test650 {
	public class Inner {
		public void foo() {
			extracted();
		}

		protected void extracted() {
			/*[*/foo();/*]*/
		}
	}
}
