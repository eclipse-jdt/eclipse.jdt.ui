package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class TestInitializerThrowsException {

	public void foo() {
		File file= null;
		
		try {
			/*[*/URL url= file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
	}
}
