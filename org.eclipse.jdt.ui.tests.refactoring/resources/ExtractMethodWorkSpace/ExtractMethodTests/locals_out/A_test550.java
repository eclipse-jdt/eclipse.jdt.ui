package locals_out;

public class A_test550 {

	public void foo() {
		int i= 0;
		while(true) {
			i = extracted(i);
		}
	}

	protected int extracted(int i) {
		/*[*/i++;/*]*/
		return i;
	}
}

