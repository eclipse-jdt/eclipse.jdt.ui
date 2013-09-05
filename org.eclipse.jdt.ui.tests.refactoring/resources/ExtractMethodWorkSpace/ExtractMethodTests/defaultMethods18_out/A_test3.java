package defaultMethods_out;

public interface A_test3 {
	interface B {
		default int foo() {
			return extracted();
		}
	}

	static int extracted() {
		/*[*/return 0;/*]*/
	}
}