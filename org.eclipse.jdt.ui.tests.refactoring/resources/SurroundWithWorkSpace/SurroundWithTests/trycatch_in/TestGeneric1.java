package trycatch_in;

import java.io.File;

public class TestGeneric1<T> {
	public void foo() {
		File file= null;
		/*[*/file.toURL();/*]*/
	}
}
