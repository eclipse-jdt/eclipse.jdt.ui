package locals_out;

public class A_test552 {

	public void foo() {
		int i= 0;
		for (;true;) {
			i = extracted(i);
		}
	}

	protected int extracted(int i) {
		/*[*/i++;/*]*/
		return i;
	}
}

