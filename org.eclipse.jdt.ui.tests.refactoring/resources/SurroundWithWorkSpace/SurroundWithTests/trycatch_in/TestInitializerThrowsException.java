package trycatch_in;

import java.io.File;
import java.net.URL;

public class TestInitializerThrowsException {

	public void foo() {
		File file= null;
		
		/*[*/URL url= file.toURL();/*]*/
	}
}
