package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

class TestDeclarationInitializer {
	public void foo() {
		File file= null;
		
		/*[*/URL url;/*]*/
		try {
			url = file.toURL();
		} catch (MalformedURLException e) {
		}
		url= null;
	}
}