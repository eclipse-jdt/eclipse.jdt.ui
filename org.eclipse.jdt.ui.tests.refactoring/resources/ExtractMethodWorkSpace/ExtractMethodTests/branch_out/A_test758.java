package branch_out;

import java.util.List;

public class A_test758 {

	public void foo(List a) {
		extracted(a);
	}

	protected void extracted(List a) {
		/*[*/
		for (Object x : a) {
			if(x == null) {
				continue;
			}
			System.out.println();
		}
		/*]*/
	}
}

