package trycatch_in;

import java.io.File;
import java.net.URL;

class TestDeclarationInitializer {
	public void foo() {
		File file= null;
		
		/*[*/URL url= file.toURL();/*]*/
		url= null;
	}
}