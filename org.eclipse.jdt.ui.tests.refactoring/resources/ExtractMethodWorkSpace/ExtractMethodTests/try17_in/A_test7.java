package try17_in;

import java.io.FileReader;
import java.io.IOException;

class A_test7 {

	void foo() throws IOException{
		try (FileReader reader = /*]*/new FileReader("file")/*[*/) {
			int ch;
			while ((ch = reader.read()) != -1) {
				System.out.println(ch);
			}
		}
	}
}