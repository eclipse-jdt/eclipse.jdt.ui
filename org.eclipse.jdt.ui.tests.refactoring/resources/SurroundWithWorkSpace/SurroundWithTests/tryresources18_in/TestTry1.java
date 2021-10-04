package trycatch18_in;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

class TestTry1 {
	FileInputStream foo() {
		try {
			/*[*/FileInputStream f = new FileInputStream("a.b");
			return f;/*]*/
		} catch (FileNotFoundException e) {
			// do nothing
		}
		return null;
	}
}
