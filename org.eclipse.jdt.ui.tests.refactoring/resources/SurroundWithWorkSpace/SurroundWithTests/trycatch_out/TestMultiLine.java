package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestMultiLine {
	public void foo() {
		File file= null;
		foo();/*]*/try {
			file.toURL();
			file.toURL();
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}foo();
	}
}
