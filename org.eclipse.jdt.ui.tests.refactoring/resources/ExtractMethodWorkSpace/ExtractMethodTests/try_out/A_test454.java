package try_out;

import java.io.IOException;

public class A_test454 {

	public void foo() {
		try {
			extracted();
		} catch (Exception e) {
		}
	}

	protected void extracted() throws IOException {
		/*[*/throw new IOException("Message");/*]*/
	}
}
