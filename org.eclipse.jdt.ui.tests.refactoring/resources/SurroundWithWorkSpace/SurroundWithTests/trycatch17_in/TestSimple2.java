package trycatch17_in;

import java.io.FileNotFoundException;

class TestSimple2 {
	void foo(int a) {
		/*[*/if (a < 10)
			throw new FileNotFoundException();/*]*/
	}
}