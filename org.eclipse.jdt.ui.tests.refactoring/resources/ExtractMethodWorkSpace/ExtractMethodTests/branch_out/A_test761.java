package branch_out;

public class A_test761 {

	public void foo() {
		outer: for (int i= 0; i < 3; i++) {
			extracted();
		}
	}

	protected void extracted() {
		/*[*/
		for (int j= 0; j < 3; j++) {
			if(j == 3) {
				return;
			}
			System.out.println();
		}
		/*]*/
	}
}

