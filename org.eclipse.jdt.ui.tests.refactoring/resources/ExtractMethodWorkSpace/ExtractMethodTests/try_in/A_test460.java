package try_in;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public abstract class A_test460 {
	public void foo() throws InvocationTargetException {
		/*[*/InputStreamReader in= null;
		try {
			bar();
		} catch (IOException e) {
			throw new InvocationTargetException(null);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}/*]*/
	}

	public abstract void bar() throws IOException;
}

