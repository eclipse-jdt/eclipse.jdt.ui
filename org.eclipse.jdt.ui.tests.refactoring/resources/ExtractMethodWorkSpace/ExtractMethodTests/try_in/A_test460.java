package try_in;

import java.io.IOException;
import java.io.InputStreamReader;

public abstract class A_test460 {
	public void foo() throws NumberFormatException {
		/*]*/InputStreamReader in= null;
		try {
			bar();
		} catch (IOException e) {
			throw new NumberFormatException();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}/*[*/
	}
	
	public abstract void bar() throws IOException;
}

