package trycatch18_in;

import java.net.Socket;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.DataFormatException;

class TestWithThrows1 {
	void foo(int a) throws FileNotFoundException {
		try (/*[*/ Socket s = new Socket();
				FileInputStream is = new FileInputStream("a.b")) {
			s.getInetAddress();/*]*/
			if (s.getTcpNoDelay())
				throw new DataFormatException();
			is.available();
		} catch (FileNotFoundException e) {
			throw e;
		} catch (DataFormatException | IOException e) {
		}
	}
}
