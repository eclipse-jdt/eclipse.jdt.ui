package try_out;

import java.io.IOException;

public class A_test452 {
	public void foo() {
		try {
			extracted();
		} catch (IOException e) {
		}
	}

	protected void extracted() throws IOException {
		/*[*/g();/*]*/
	}

	public void g() throws IOException {
	}
}
