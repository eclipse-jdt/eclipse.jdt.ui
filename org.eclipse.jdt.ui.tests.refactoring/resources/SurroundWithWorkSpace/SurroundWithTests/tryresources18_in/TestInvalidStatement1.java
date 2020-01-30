package trycatch18_in;

import java.net.Socket;

class TestInvalidStatement1 {
	void foo(int a) {
		/*[*/Socket s=/*]*/new Socket();
	}
}
