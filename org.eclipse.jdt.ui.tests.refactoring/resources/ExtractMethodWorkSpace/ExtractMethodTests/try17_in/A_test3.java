package try17_in;

import java.io.FileReader;

public class A_test3 {

	void foo() throws Exception {
		/*[*/try (FileReader reader1 = new FileReader("file")) {
			int ch;
			while ((ch = reader1.read()) != -1) {
				System.out.println(ch);
			}
		}/*]*/
	}
}
