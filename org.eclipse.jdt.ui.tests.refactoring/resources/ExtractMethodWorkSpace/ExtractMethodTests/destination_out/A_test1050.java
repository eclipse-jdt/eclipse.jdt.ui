package destination_out;

public class A_test1050 {
	public class Inner {
		public void foo() {
			extracted();
		}
	}
	public void bar() {
	}
	protected void extracted() {
		/*[*/bar();/*]*/
	}
}
