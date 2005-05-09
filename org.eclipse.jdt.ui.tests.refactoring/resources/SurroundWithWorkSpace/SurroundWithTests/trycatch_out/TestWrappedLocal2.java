package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal2 {

	public void foo() {
		File file= null;

		/*[*/int i;
		try {
			i = 10; int x; 
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i= 20;
	}
}
