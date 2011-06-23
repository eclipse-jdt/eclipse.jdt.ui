package try17_in;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;

public class A_test1 {
	public void foo(int a) throws Exception {
		/*[*/try {
			if (a < 10)
				throw new FileNotFoundException();
			else if (a < 20)
				throw new InterruptedIOException();
			else
				throw new IOException();
		} catch (FileNotFoundException | InterruptedIOException ex) {
			ex.printStackTrace();
		}/*]*/
	}
}
