package destination_out;

public class A_test1053 {
	public static class Inner {
		public void foo() {
			extracted();
		}
	}

	public static void bar() {
		System.out.println();
	}

	protected static void extracted() {
		/*[*/bar();/*]*/
	}
}