package trycatch_in;

import java.io.File;

public enum TestEnum1 {
	A;
	public void foo() {
		File file= null;
		/*[*/file.toURL();/*]*/
	}
}
