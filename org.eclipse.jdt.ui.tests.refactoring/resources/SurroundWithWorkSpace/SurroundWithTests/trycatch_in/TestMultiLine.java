package trycatch_in;

import java.io.File;

public class TestMultiLine {
	public void foo() {
		File file= null;
		foo();/*]*/file.toURL();
		file.toURL();
		file.toURL();/*]*/foo();
	}
}
