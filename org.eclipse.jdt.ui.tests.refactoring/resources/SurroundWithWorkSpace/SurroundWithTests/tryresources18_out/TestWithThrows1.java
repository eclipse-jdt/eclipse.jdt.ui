package trycatch18_in;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;

class TestWithThrows1 {
	void foo(int a) {
		try (/*[*/ Socket s = new Socket();
				InputStream is = s.getInputStream()) {
			s.getInetAddress();/*]*/
			if (s.getTcpNoDelay())
				throw new DataFormatException();
			is.available();
		} catch (IOException | DataFormatException e) {
		}
	}
}
