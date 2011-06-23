package try17_out;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;

public class A_test2 {
	public void foo(int a) throws Exception {
		try {
			if (a < 10)
				throw new FileNotFoundException();
			else if (a < 20)
				throw new InterruptedIOException();
		} catch (FileNotFoundException | InterruptedIOException ex) {
			extracted(ex);
		}
	}

	protected void extracted(IOException ex) {
		/*[*/ex.printStackTrace();/*]*/
	}
}
