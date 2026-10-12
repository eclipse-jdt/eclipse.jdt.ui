package validSelection_in;

import java.util.ArrayList;

public class A_testIssue3234 {

	public int foo() {
		/*]*/return extracted();/*[*/
	}

	protected int extracted() {
		throw new IllegalStateException("error");
	}

}
