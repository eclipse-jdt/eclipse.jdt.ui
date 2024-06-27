package nested_in;

public class A_test655 {
	public static class NestedStatic {
		private int i= 0;

		public void foo() {
			extracted(this);
		}
	}

	protected static void extracted(NestedStatic passedNestedStatic) {
		/*[*/System.out.println("Greetings" + ++passedNestedStatic.i);/*]*/
	}
}
