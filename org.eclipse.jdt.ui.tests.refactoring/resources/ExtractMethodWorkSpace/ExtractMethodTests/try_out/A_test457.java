package try_out;

import java.io.IOException;

public class A_test457 {

	public void foo() {
		Exception[] e= new Exception[] { new IOException("Message") };
		try {
			extracted(e);
		} catch (Exception x) {
		}
	}

	protected void extracted(Exception[] e) throws Exception {
		/*[*/throw e[0];/*]*/
	}
}
