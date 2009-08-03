package branch_out;

public class A_test766 {

	public void foo() {
		int x = 0;
		foo: for (int i= 0; i < 3; i++) {
			x = extracted(x, i);
		}
		System.out.println(x);
	}

	protected int extracted(int x, int i) {
		/*[*/
		if(i == 2) {
			x = (i*3);
			return x;
		}
		System.out.println();
		/*]*/
		return x;
	}
}

