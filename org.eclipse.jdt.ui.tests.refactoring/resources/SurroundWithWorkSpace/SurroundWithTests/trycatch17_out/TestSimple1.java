package trycatch17_out;

import java.io.FileNotFoundException;
import java.io.InterruptedIOException;

class TestSimple1 {
	void foo(int a) {
		try {
			/*[*/if (a < 10)
				throw new FileNotFoundException();
			else
				throw new InterruptedIOException();/*]*/
		} catch (FileNotFoundException | InterruptedIOException e) {
		}
	}
}