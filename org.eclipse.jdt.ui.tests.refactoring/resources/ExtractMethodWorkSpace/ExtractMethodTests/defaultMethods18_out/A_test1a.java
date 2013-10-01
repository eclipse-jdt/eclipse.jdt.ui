package defaultMethods_out;

public interface A_test1a {
	public default int foo() {
		return extracted();
	}

	public default int extracted() {
		/*[*/return 0;/*]*/
	}
}
