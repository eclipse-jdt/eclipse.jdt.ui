package return_out;

public class A_test731 {
	public void foo(int x) {
		for (int i = 0; i < 3; i++) {
		}
		
		int a = 0;
		do {
			a = extracted(a);
		} while (x==3);
	}

	protected int extracted(int a) {
		/*[*/g(a++);/*]*/
		return a;
	}

	private void g(int i) {}
}

