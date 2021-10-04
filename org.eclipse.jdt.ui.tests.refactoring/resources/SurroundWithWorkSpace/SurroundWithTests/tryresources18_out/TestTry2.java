package trycatch18_out;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class TestTry1 {
	FileInputStream foo() {
		try {
			String name = "a.b";
			try (/*[*/ FileInputStream f = new FileInputStream(name)) {
				return f;/*]*/
			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException e) {
			}
		} catch (FileNotFoundException e) {
			// do nothing
		}
		return null;
	}
}
