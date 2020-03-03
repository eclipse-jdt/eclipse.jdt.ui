package trycatch18_in;

import java.net.Socket;
import java.io.InputStream;

class TestWithException1 {
	void foo(int a) {
		/*[*/Socket s=new Socket();
		InputStream is=s.getInputStream();
		Integer x= Integer.valueOf("123");
		s.getInetAddress();/*]*/
		is.available();
	}
}
