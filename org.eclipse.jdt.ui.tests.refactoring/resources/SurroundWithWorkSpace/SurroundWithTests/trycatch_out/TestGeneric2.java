package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestGeneric2 {
	public <T> void foo() {
		File file= null;
		try {
			/*[*/file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
	}
}
