package try17_out;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

class A_test7 {

	void foo() throws IOException{
		try (FileReader reader = /*]*/extracted()/*[*/) {
			int ch;
			while ((ch = reader.read()) != -1) {
				System.out.println(ch);
			}
		}
	}

	protected FileReader extracted() throws FileNotFoundException {
		return new FileReader("file");
	}
}