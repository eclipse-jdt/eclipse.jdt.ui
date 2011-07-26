package p; //9, 34, 9, 56

import java.io.FileReader;
import java.io.IOException;

class A {

	void foo() throws IOException{
		FileReader fileReader= new FileReader("file");
		try (FileReader reader = fileReader) {
			int ch;
			while ((ch = reader.read()) != -1) {
				System.out.println(ch);
			}
		}
	}
}