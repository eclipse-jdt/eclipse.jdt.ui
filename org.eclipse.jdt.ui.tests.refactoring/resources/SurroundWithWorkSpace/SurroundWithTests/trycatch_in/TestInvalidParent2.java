package trycatch_in;

import java.io.File;
import java.net.URL;

public class TestInvalidParent2 {
	public void foo() {
		File file= null;
		URL url= /*]*/file.toURL();/*[*/
	}
}
