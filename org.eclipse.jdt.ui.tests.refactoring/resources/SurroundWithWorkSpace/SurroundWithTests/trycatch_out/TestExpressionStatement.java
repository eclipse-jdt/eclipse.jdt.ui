package trycatch_out;

import java.net.MalformedURLException;
import java.net.URL;

public class TestExpressionStatement {
	public void foo() {
		try {
			/*[*/new URL("http://www.eclipse.org")/*]*/;
		} catch (MalformedURLException e) {
		}
	}
}
