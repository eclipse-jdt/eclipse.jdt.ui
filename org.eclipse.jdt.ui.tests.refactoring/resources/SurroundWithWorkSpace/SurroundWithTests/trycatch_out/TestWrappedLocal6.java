package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal6 {

	public void foo() {
		File file= null;

		/*[*/int a, i, x;
		try {
			i = 10;
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i= 20;
	}

}
