package defaultMethods_out;

public interface A_test3a {
	interface B {
		public default int foo() {
			return extracted();
		}
	}

	static int extracted() {
		/*[*/return 0;/*]*/
	}
}