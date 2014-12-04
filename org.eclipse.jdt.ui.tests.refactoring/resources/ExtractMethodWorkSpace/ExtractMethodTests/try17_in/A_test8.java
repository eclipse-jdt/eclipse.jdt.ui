package try17_in;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class A_test8 {
	String readFirstLineFromFile(String path) throws IOException {
		/*]*/try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			return br.readLine();
		}/*[*/
	}
}