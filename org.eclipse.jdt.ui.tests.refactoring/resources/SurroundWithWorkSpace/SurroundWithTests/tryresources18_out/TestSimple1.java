package trycatch18_out;

import java.io.IOException;
import java.net.Socket;

class TestSimple1 {
	void foo(int a) {
		try (/*[*/ Socket s = new Socket()) {
			s.getInetAddress();/*]*/
		} catch (IOException e) {
		}
	}
}
