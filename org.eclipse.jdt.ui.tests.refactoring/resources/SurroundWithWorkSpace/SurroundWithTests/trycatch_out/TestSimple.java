package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestSimple {
	public void foo() {
		File file= null;
		try {
			/*[*/file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
	}
}
