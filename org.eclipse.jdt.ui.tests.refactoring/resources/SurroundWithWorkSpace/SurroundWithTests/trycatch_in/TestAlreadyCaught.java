package trycatch_in;

import java.io.File;
import java.net.MalformedURLException;

public class TestAlreadyCaught {
	public void foo() {
		File file= null;
		try {
			/*]*/file.toURL();/*[*/
		} catch(MalformedURLException e) {
		}
	}
}
