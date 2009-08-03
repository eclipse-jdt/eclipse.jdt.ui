package branch_out;

public class A_test760 {

	public void foo(int a) {
		extracted(a);
	}

	protected void extracted(int a) {
		/*[*/
		do {
			if(a == 3) {
				continue;
			}
			System.out.println();
		} while (a > 0);
		/*]*/
	}
}

