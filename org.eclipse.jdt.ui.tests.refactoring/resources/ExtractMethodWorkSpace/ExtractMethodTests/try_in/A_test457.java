package try_in;

import java.io.IOException;

public class A_test457 {

	public void foo() {
		Exception[] e= new Exception[] { new IOException("Message") };
		try {
			/*[*/throw e[0];/*]*/
		} catch (Exception x) {
		}
	}
}
