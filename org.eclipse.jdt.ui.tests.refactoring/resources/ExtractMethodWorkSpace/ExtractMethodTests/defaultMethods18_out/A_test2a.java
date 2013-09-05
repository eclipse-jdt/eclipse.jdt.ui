package defaultMethods_out;

public class A_test2a {
	interface B {
		public default int foo() {
			return extracted();
		}
	}

	protected static int extracted() {
		/*[*/return 0;/*]*/
	}
}