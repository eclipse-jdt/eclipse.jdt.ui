package trycatch18_out;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class TestTry1 {
	FileInputStream foo() {
		try (/*[*/ FileInputStream f = new FileInputStream("a.b")) {
			return f;/*]*/
		} catch (FileNotFoundException e) {
			// do nothing
		} catch (IOException e) {
		}
		return null;
	}
}
