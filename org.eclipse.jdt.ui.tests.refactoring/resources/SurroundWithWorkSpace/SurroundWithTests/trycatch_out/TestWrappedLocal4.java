package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal4 {

	public void foo() {
		File file= null;
		
		/*[*/int i, x;
		try {
			i = 10;
			x = 20;
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i= 20;
	}

}
