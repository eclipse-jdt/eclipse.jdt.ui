package try_in;

import java.io.IOException;

public class A_test456 {

	public void foo() {
		Exception e= new IOException("Message");
		try {
			/*[*/throw e;/*]*/
		} catch (Exception x) {
		}
	}
}
