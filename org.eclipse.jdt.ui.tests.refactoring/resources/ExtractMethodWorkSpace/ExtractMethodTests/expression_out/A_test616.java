package expression_out;

import java.io.File;

public class A_test616 {
	public void foo() {
		A a= null;
		/*]*/extracted(a).getName();
	}

	protected File extracted(A a) {
		return a.getFile()/*]*/;
	}
}
