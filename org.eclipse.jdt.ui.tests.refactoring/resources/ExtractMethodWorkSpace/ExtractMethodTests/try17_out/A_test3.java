package try17_out;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class A_test3 {

	void foo() throws Exception {
		extracted();
	}

	protected void extracted() throws IOException, FileNotFoundException {
		/*[*/try (FileReader reader1 = new FileReader("file")) {
			int ch;
			while ((ch = reader1.read()) != -1) {
				System.out.println(ch);
			}
		}/*]*/
	}
}
