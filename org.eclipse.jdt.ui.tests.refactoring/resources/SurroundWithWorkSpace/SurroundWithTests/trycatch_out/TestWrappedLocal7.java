package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal7 {

	public void foo() {
		/*[*/File file;
		File file2;
		try {
			file = null;
			file.toURL();
			file2 = null;
		} catch (MalformedURLException e) {
		}
		file= null;
		file2= null;
	}

}
