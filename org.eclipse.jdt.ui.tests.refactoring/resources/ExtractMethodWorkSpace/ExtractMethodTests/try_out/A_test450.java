package try_out;

import java.io.IOException;

public class A_test450 {
	public void foo() {
		try {
			extracted();
		} catch (java.io.IOException e) {
		}
	}
	protected void extracted() throws IOException {
		/*[*/g();/*]*/
	}
	public void g() throws java.io.IOException {
	}
}
