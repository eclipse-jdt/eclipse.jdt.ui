package try17_out;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

class A_test8 {
	String readFirstLineFromFile(String path) throws IOException {
		/*]*/return extracted(path);/*[*/
	}

	protected String extracted(String path) throws IOException, FileNotFoundException {
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			return br.readLine();
		}
	}
}