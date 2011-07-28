import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class A_testTryWithResources_in {

	void foo() throws IOException{
		try (InputStreamReader reader = new FileReader("file")) {
			int ch;
			while ((ch = reader.read()) != -1) {
				System.out.println(ch);
			}
		}
	}
}