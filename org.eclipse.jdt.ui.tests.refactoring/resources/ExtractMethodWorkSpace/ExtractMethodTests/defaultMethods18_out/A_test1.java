package defaultMethods_out;

public interface A_test1 {
	default int foo() {
		return extracted();
	}

	default int extracted() {
		/*[*/return 0;/*]*/
	}
}
