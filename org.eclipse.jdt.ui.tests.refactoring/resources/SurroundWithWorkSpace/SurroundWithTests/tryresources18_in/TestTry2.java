package trycatch18_in;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

class TestTry1 {
	FileInputStream foo() {
		try {
			String name = "a.b";
			/*[*/FileInputStream f = new FileInputStream(name);
			return f;/*]*/
		} catch (FileNotFoundException e) {
			// do nothing
		}
		return null;
	}
}
