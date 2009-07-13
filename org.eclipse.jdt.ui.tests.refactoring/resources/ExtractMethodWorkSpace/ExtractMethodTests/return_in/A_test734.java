package return_in;

public class A_test734 {
	public int foo() {
		/*[*/
		int x = 1;
		if (x > 0) {
			return x;
		}
		throw new IllegalArgumentException();
		/*]*/
	}
}

