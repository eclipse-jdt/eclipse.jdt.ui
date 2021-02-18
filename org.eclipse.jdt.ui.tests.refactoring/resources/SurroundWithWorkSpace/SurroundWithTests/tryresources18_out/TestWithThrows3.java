package trycatch18_in;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class TestWithThrows3 {
	public FileInputStream foo(int a) throws FileNotFoundException {
		try {
			try (/*[*/ FileInputStream f = new FileInputStream("a.b")) {
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
