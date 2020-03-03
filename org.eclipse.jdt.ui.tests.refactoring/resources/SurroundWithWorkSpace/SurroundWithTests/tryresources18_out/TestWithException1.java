package trycatch18_out;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;

class TestWithException1 {
	void foo(int a) {
		try (/*[*/ Socket s = new Socket();
				InputStream is = s.getInputStream()) {
			s.getInetAddress();/*]*/
			is.available();
		} catch (IOException e) {
		}
	}
}
