package return_out;

public class A_test730 {
	public void foo(int x) {
		do {
		} while (x==3);
		
		int a = 0;
		for (int i = 0; i < 3; i++) {
			i = extracted(i);
		}
	}

	protected int extracted(int i) {
		/*[*/g(i++);/*]*/
		return i;
	}

	private void g(int i) {}
}

