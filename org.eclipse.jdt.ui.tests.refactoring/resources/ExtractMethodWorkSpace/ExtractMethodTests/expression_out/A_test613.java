package expression_out;

import java.io.File;

public class A_test613 {
	public void foo() {
		A a= null;
		a.useFile(extracted(a));
	}

	protected File extracted(A a) {
		return /*[*/a.getFile()/*]*/;
	}
}
