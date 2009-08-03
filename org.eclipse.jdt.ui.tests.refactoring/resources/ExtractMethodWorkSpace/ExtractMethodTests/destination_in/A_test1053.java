package destination_in;

public class A_test1053 {
	public static class Inner {
		public void foo() {
			/*[*/bar();/*]*/
		}
	}

	public static void bar() {
		System.out.println();
	}
}