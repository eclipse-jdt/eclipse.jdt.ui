package expression_out;

public class A_test630 {

	static int test() {
		byte b= extracted();
		return b * 10;
	}

	protected static byte extracted() {
		return /*[*/1 + 2/*]*/;
	}
}
