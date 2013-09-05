package defaultMethods_out;

public class A_test2 {
	interface B {
		default int foo() {
			return extracted();
		}
	}

	protected static int extracted() {
		/*[*/return 0;/*]*/
	}
}