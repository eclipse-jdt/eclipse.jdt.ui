package branch_out;

public class A_test767 {

	public void foo() {
		for (int i = 0; i < 3; i++) {
			extracted();
		}
	}

	protected void extracted() {
		/*[*/
		inner: for (int j = 0; j < 10; j++) {
			if (j == 2) {
				System.out.println();
				continue inner;
			}
		}
		/*]*/
	}
}

