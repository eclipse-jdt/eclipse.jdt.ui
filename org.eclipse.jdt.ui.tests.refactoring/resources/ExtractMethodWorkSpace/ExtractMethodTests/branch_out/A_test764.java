package branch_out;

public class A_test764 {

	public void foo() {
		int x = 0;
		for (int i= 0; i < 3; i++) {
			x = extracted(x, i);
		}
		System.out.println(x);
	}

	protected int extracted(int x, int i) {
		/*[*/
		if(i == 2) {
			x = 2;
			return x;
		}
		System.out.println();
		/*]*/
		return x;
	}
}

