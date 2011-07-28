package trycatch17_out;

import java.io.FileNotFoundException;

class TestSimple2 {
	void foo(int a) {
		try {
			/*[*/if (a < 10)
				throw new FileNotFoundException();/*]*/
		} catch (FileNotFoundException e) {
		}
	}
}