package duplicates_out;

public class A_test957 {
	public void f() {
		int i = 17;
		int k = 1;

		i = extracted(i);
		k = extracted(k);

		System.out.println(i);
		System.out.println(k);
	}

	protected int extracted(int i) {
		/*[*/i++;/*]*/
		return i;
	}
}
