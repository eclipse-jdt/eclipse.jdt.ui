package defaultMethods_out;

public interface A_test5 {
	default int foo() {
		return extracted();
	}

	static int extracted() {
		/*[*/return 0;/*]*/
	}
	
	static int foo() {
		return extracted();
	}
}
