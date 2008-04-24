package trycatch_in;

import java.io.File;
import java.net.MalformedURLException;

public class TestAlreadyCaught {
	public void foo() {
		File file= null;
		try {
			/*]*/try {
				file.toURL();
			} catch (Exception e) {
			}/*[*/
		} catch(MalformedURLException e) {
		}
	}
}
