package locals_out;

public class A_test577 {
	public void foo() {
		int x = 0;
		for (int i = x; i < 10; i++)
			x = extracted(x, i);
	}

	protected int extracted(int x, int i) {
		/*[*/
		bar(i, x++);
		/*]*/
		return x;
	}
	
	private void bar(int i, int y) {
	}
}

