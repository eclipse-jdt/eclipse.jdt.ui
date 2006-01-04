package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal8 {

	public void foo() {
		/*[*/File file;
		try {
			file = null;
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		
		File file2= file;
	}

}
