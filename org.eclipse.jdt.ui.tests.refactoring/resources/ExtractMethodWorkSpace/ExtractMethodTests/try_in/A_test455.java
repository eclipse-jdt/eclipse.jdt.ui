package try_in;

import java.io.IOException;

public class A_test455 {

	public void foo() {
		try {
			/*[*/throw createException();/*]*/
		} catch (Exception e) {
		}
	}

	public IOException createException() {
		return new IOException("Message");
	}
}