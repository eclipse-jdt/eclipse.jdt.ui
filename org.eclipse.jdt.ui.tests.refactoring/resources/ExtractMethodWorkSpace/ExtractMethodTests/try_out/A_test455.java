package try_out;

import java.io.IOException;

public class A_test455 {

	public void foo() {
		try {
			extracted();
		} catch (Exception e) {
		}
	}

	protected void extracted() throws IOException {
		/*[*/throw createException();/*]*/
	}

	public IOException createException() {
		return new IOException("Message");
	}
}