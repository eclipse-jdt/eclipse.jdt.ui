package return_in;

public class A_test735 {
	public void foo() {
		int list[] = new int[3];
		for (int i = 0; i < 1; ++i) {
			int b = 0;
			for (int count = 0; count < 3; count++) {
				b = extracted(list, b);
			}
		}
		System.out.println(list);
	}

	protected int extracted(int[] list, int b) {
		/*[*/
		list[b] = b++;
		/*]*/
		return b;
	}
}
