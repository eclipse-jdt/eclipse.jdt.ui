package tryresources18_out;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

class TestSimple3 {
	void foo(int a) {
		try (/*[*//*1*/ Socket s = new Socket();
				Socket s2 = new Socket();
				/*2*/ InputStream is = s.getInputStream()) {
			s.getInetAddress();/*]*/
			s2.getInetAddress();
			System.out.println(s.getInetAddress().toString());
			/*3*/int i = 0;
			System.out.println(is.markSupported());/*0*/
		} catch (IOException e) {
		}
	}
}
