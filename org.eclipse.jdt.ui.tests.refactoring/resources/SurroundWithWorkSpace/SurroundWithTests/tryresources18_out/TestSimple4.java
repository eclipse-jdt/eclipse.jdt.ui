package trycatch18_out;

import java.io.IOException;
import java.net.Socket;

class TestSimple4 {
	void foo(int a) {
		try (/*[*/ Socket s = new Socket()) {
		} catch (IOException e) {
		}
	}
}
