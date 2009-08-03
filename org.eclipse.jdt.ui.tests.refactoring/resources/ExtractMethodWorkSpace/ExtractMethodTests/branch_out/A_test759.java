package branch_out;

public class A_test759 {

	public void foo(int a) {
		extracted(a);
	}

	protected void extracted(int a) {
		/*[*/
		while (a > 0) {
			if(a == 3) {
				continue;
			}
			System.out.println();
		}
		/*]*/
	}
}

