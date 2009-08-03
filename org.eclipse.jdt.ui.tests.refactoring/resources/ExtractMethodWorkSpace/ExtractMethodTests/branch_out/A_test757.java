package branch_out;

public class A_test757 {

	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/
		for (int i= 0; i < 3; i++) {
			if(i == 2) {
				continue;
			}
			System.out.println();
		}
		/*]*/
	}
}

