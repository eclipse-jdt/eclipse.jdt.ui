package locals_out;

public class A_test551 {

	public void foo() {
		int i= 0;
		do {
			i = extracted(i);
		} while (true);
	}

	protected int extracted(int i) {
		/*[*/i++;/*]*/
		return i;
	}
}

