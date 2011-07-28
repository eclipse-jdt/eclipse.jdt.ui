package trycatch17_in;

import java.io.FileNotFoundException;
import java.io.InterruptedIOException;

class TestSimple1 {
	void foo(int a) {
		/*[*/if (a < 10)
			throw new FileNotFoundException();
		else
			throw new InterruptedIOException();/*]*/
	}
}