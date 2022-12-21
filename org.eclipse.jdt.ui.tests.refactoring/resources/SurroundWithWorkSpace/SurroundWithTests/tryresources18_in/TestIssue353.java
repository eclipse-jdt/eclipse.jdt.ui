package trycatch18_in;

import java.net.Socket;

class TestIssue353 {
	void foo(int a) {
		/*[*/Socket s=new Socket();
		s.getInetAddress();/*]*/
		
		try {
		} catch (Exception e) {
		}
	}
}
