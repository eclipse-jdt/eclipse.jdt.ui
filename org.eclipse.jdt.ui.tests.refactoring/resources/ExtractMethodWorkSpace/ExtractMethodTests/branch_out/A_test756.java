package branch_out;

public class A_test756 {

	public void foo() {
		for (int i= 0; i < 3; i++) {
			extracted(i);
		}
	}

	protected void extracted(int i) {
		/*[*/
		if(i == 2) {
			return;
		}
		System.out.println();
		/*]*/
	}
}

