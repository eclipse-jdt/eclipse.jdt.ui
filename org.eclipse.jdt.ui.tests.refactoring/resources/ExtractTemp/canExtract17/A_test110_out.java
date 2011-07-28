package p; //14, 13, 14, 15

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;

class A {
	public void foo(int a) throws Exception {
		try {
			if (a < 10)
				throw new FileNotFoundException();
			else if (a < 20)
				throw new InterruptedIOException();
		} catch (FileNotFoundException | InterruptedIOException ex) {
			IOException temp= ex;
			temp.printStackTrace();
		}
	}
}
