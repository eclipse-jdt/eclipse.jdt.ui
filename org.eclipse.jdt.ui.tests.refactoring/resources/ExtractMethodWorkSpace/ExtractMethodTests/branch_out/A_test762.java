package branch_out;

public class A_test762 {

	public void foo() {
		outer: for (int i= 0; i < 3; i++) {
			extracted();
		}
	}

	protected void extracted() {
		/*[*/
		for (int j= 0; j < 3; j++) {
			for (int k= 0; k < 3; k++) {
				if(j == 3) {
					return;
				}
				System.out.println();
			}
		}
		/*]*/
	}
}

