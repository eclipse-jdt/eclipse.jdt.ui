package trycatch_in;

import java.io.File;

public class TestOneLine {
	public void foo() {
		File file= null;
		foo();/*]*/file.toURL();/*]*/foo();
	}
}
