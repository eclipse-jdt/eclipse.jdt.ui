package return_in;

public class A_test733 {
	public int foo() {
		/*[*/
		int x = 1;
		if (x > 0) {
			throw new IllegalArgumentException();
		}
		return x;
		/*]*/
	}
}

