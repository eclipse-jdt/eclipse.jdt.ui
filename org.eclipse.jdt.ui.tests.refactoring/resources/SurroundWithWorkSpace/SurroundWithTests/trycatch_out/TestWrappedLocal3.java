package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal3 {

	public void foo() {
		File file= null;

		int i;
		try {
			/*[*/int x;
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i= 20;
	}

}
