package validSelection_in;

import java.util.ArrayList;

public class A_testIssue3234 {

	public int foo() {
		/*]*/throw new IllegalStateException("error");/*[*/
	}

}
