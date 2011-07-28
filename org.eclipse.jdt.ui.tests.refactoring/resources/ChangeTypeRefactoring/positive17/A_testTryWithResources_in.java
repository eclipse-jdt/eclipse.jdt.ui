import java.io.FileReader;
import java.io.IOException;

public class A_testTryWithResources_in {

	void foo() throws IOException{
		try (FileReader reader = new FileReader("file")) {
			int ch;
			while ((ch = reader.read()) != -1) {
				System.out.println(ch);
			}
		}
	}
}