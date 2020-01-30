package tryresources18_out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TryTest2 {
	
	@SuppressWarnings("unused")
	
	public void read() throws IOException {
		try (// create socket
				/*[*//* 1 */ Socket s = new Socket()) {
			// create reader
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			// use reader
			int data = br.read();
		}
	}

}
