package try_out;

import java.io.IOException;

public class A_test453 {

	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/try {
			g();
		} catch (Exception e) {
		}/*]*/
	}

	public void g() throws IOException {
	}
}
