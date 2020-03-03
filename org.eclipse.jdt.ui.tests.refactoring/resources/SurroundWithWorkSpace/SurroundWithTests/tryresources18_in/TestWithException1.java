package trycatch18_in;

import java.net.Socket;
import java.io.InputStream;

class TestWithException1 {
	void foo(int a) {
		/*[*/Socket s=new Socket();
		InputStream is=s.getInputStream();
		s.getInetAddress();/*]*/
		is.available();
	}
}
