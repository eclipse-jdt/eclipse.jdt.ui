package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal5 {

	public void foo() {
		File file= null;

		/*[*/int i, x;
		try {
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i= 20;
	}

}
