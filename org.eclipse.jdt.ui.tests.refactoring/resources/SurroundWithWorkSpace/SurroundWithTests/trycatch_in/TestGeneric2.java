package trycatch_in;

import java.io.File;

public class TestGeneric2 {
	public <T> void foo() {
		File file= null;
		/*[*/file.toURL();/*]*/
	}
}
