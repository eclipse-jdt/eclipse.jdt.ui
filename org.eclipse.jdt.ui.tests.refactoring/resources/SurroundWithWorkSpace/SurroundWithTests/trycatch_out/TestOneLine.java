package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestOneLine {
	public void foo() {
		File file= null;
		foo();/*]*/try {
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}foo();
	}
}
