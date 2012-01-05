//selection: 16, 13, 16, 15
//name: exception -> second
package simple;

import java.io.FileNotFoundException;
import java.io.InterruptedIOException;

public class Catch2 {
	public void foo(int a) throws Exception {
		try {
			if (a < 10)
				throw new FileNotFoundException();
			else if (a < 20)
				throw new InterruptedIOException();
		} catch (FileNotFoundException | InterruptedIOException ex) {
			ex.printStackTrace();
		}
	}
}
